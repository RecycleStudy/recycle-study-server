"""
Email scheduler benchmark - repeated measurements with container restart.

Usage:
  python perftest/run-benchmark.py
  python perftest/run-benchmark.py --iterations 3 --counts 50,200
  python perftest/run-benchmark.py --iterations 5 --counts 50,200,500

Each iteration: restart app container -> health check -> cleanup -> insert -> wait -> measure
"""

import subprocess
import sys
import time
import re
import json
import argparse
import urllib.request
from datetime import datetime

DOCKER_COMPOSE = "perftest/docker-compose.yml"
APP_CONTAINER = "perftest-app"
RESULTS_FILE = "perftest/benchmark-results.json"


def run_cmd(cmd, timeout=120):
    result = subprocess.run(
        cmd, shell=True, capture_output=True, text=True,
        timeout=timeout, encoding="utf-8", errors="replace"
    )
    return result.stdout.strip(), result.stderr.strip(), result.returncode


def log(msg):
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] {msg}", flush=True)


def restart_app():
    log("Restarting app container...")
    run_cmd(f"docker compose -f {DOCKER_COMPOSE} stop app", timeout=30)
    run_cmd(f"docker compose -f {DOCKER_COMPOSE} rm -f app", timeout=15)
    run_cmd(f"docker compose -f {DOCKER_COMPOSE} up -d app", timeout=60)


def wait_for_health(timeout=120):
    log("Waiting for health check...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            resp = urllib.request.urlopen(
                "http://localhost:8080/actuator/health", timeout=5
            )
            if resp.getcode() == 200:
                elapsed = int(time.time() - start)
                log(f"App healthy ({elapsed}s)")
                return True
        except Exception:
            pass
        time.sleep(2)
    log("ERROR: Health check timed out")
    return False


def cleanup_data():
    log("Cleaning up data...")
    out, err, rc = run_cmd("python perftest/generate-test-data.py cleanup")
    if rc != 0:
        log(f"Cleanup warning: {err}")


def insert_data(count, minutes_ahead=2):
    log(f"Inserting {count} records (scheduled in {minutes_ahead}min)...")
    out, err, rc = run_cmd(
        f"python perftest/generate-test-data.py scheduler --count {count} --minutes {minutes_ahead}"
    )
    if rc != 0:
        log(f"ERROR inserting data: {err}")
        return None
    # Parse target scheduledAt
    for line in out.split("\n"):
        if "Target scheduledAt:" in line:
            m = re.search(r"(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})", line)
            if m:
                log(f"Target: {m.group(1)}")
                return m.group(1)
    log(f"Insert output:\n{out}")
    return "unknown"


def get_docker_logs():
    out, _, _ = run_cmd(f"docker logs {APP_CONTAINER} 2>&1", timeout=30)
    return out


def wait_for_completion(count, timeout=600):
    log(f"Waiting for {count} MAIL_SENT entries...")
    start = time.time()
    last_report = 0

    while time.time() - start < timeout:
        logs = get_docker_logs()
        mail_lines = [l for l in logs.split("\n") if "[MAIL_SENT]" in l and "delay=" in l]
        current = len(mail_lines)

        if current >= count:
            submit_lines = [l for l in logs.split("\n") if f"발송 요청 완료: size={count}" in l]
            if submit_lines:
                log(f"Done: {current}/{count} processed")
                return logs

        elapsed = int(time.time() - start)
        if elapsed - last_report >= 10:
            log(f"  {current}/{count} ({elapsed}s)")
            last_report = elapsed

        time.sleep(3)

    log(f"TIMEOUT after {timeout}s")
    return None


def parse_results(logs, count):
    lines = logs.split("\n")

    # Find submit complete timestamp - must match the correct count
    # The scheduler runs every minute, so during long tests (200건, 500건),
    # subsequent scheduler invocations may log "발송 요청 완료: size=0".
    # We need the line with size=count (the actual processing run).
    submit_ts = None
    for line in lines:
        if f"발송 요청 완료: size={count}" in line:
            m = re.search(r"(\d{2}:\d{2}:\d{2}\.\d{3})", line)
            if m:
                submit_ts = m.group(1)
                break  # Take the first match (should be the only one)

    if not submit_ts:
        # Fallback: try "발송 시작" with matching size
        for line in lines:
            if f"발송 시작:" in line and f"size={count}" in line:
                m = re.search(r"(\d{2}:\d{2}:\d{2}\.\d{3})", line)
                if m:
                    submit_ts = m.group(1)
                    log(f"Using 발송 시작 timestamp as fallback: {submit_ts}")
                    break

    if not submit_ts:
        log("ERROR: submit timestamp not found")
        return None

    # Parse MAIL_SENT entries
    mail_lines = [l for l in lines if "[MAIL_SENT]" in l and "delay=" in l]
    delays = []
    timestamps = []
    thread_ids = set()

    for line in mail_lines:
        dm = re.search(r"delay=(\d+)ms", line)
        if dm:
            delays.append(int(dm.group(1)))
        tm = re.search(r"(\d{2}:\d{2}:\d{2}\.\d{3})", line)
        if tm:
            timestamps.append(tm.group(1))
        tid = re.search(r"threadId=(\d+)", line)
        if tid:
            thread_ids.add(int(tid.group(1)))

    if len(delays) < count:
        log(f"ERROR: only {len(delays)}/{count} MAIL_SENT entries")
        return None

    # Use only the first `count` entries (in case of extra)
    delays = delays[:count]
    timestamps = timestamps[:count]

    def ts_to_sec(ts):
        p = ts.split(":")
        return int(p[0]) * 3600 + int(p[1]) * 60 + float(p[2])

    duration = ts_to_sec(timestamps[-1]) - ts_to_sec(submit_ts)
    throughput = count / duration
    avg_delay = sum(delays) / len(delays)

    return {
        "count": count,
        "duration": round(duration, 2),
        "throughput": round(throughput, 4),
        "avg_delay": round(avg_delay, 1),
        "min_delay": min(delays),
        "max_delay": max(delays),
        "threads": len(thread_ids),
        "submit_ts": submit_ts,
        "first_ts": timestamps[0],
        "last_ts": timestamps[-1],
    }


def run_single(count, minutes_ahead=2):
    restart_app()

    if not wait_for_health():
        return None

    cleanup_data()
    target = insert_data(count, minutes_ahead)
    if not target:
        return None

    # Estimate timeout: (count/8)*3.5 + 180 (for scheduler wait)
    processing_time = (count / 8) * 3.5
    timeout = int(processing_time + 180)
    logs = wait_for_completion(count, timeout=timeout)
    if not logs:
        return None

    result = parse_results(logs, count)
    if result:
        log(f"=> {result['duration']}s, {result['throughput']:.3f}/s, "
            f"avg_delay={result['avg_delay']}ms, threads={result['threads']}")
    return result


def print_summary(all_results):
    theory = 8 / 3.163

    print(f"\n{'=' * 70}")
    print("  BENCHMARK RESULTS")
    print(f"{'=' * 70}\n")

    summary = {}

    for count, results in sorted(all_results.items()):
        if not results:
            print(f"[{count}건] No successful runs\n")
            continue

        tps = [r["throughput"] for r in results]
        durs = [r["duration"] for r in results]
        delays = [r["avg_delay"] for r in results]
        n = len(tps)

        tp_mean = sum(tps) / n
        tp_stdev = (sum((t - tp_mean) ** 2 for t in tps) / max(n - 1, 1)) ** 0.5
        dur_mean = sum(durs) / n
        delay_mean = sum(delays) / n
        diff_pct = (tp_mean - theory) / theory * 100

        summary[count] = {
            "n": n,
            "tp_mean": round(tp_mean, 3),
            "tp_stdev": round(tp_stdev, 3),
            "tp_min": round(min(tps), 3),
            "tp_max": round(max(tps), 3),
            "dur_mean": round(dur_mean, 1),
            "delay_mean": round(delay_mean, 1),
            "diff_pct": round(diff_pct, 1),
        }

        print(f"[{count}건] ({n} runs)")
        print(f"  Throughput:  {tp_mean:.3f} +/- {tp_stdev:.3f} 건/초")
        print(f"  Theory:      {theory:.3f} 건/초")
        print(f"  Diff:        {diff_pct:+.1f}%")
        print(f"  Duration:    {dur_mean:.1f}s (min={min(durs)}, max={max(durs)})")
        print(f"  Avg delay:   {delay_mean:.1f}ms")
        print(f"  Each run:    {[r['throughput'] for r in results]}")
        print()

    # Blog-format table
    print(f"{'=' * 70}")
    print("  BLOG TABLE FORMAT")
    print(f"{'=' * 70}\n")
    print(f"| 건수 | 처리 시간 | 처리량 (건/초) | 성공률 | 1분 초과 |")
    print(f"|------|---------|------------|------|--------|")
    for count in sorted(summary.keys()):
        s = summary[count]
        dur = s["dur_mean"]
        tp = s["tp_mean"]
        exceed = f"{dur / 60:.1f}배 초과" if dur > 60 else "미초과"
        print(f"| {count:<4} | {dur:.1f}초 | {tp:.2f} | 100% | {exceed} |")

    print()
    return summary


def main():
    parser = argparse.ArgumentParser(description="Email scheduler benchmark")
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--counts", type=str, default="50,200,500")
    parser.add_argument("--minutes", type=int, default=2, help="Minutes ahead for scheduledAt")
    args = parser.parse_args()

    counts = [int(c) for c in args.counts.split(",")]
    iterations = args.iterations

    print(f"=== Email Scheduler Benchmark ===")
    print(f"Counts: {counts}")
    print(f"Iterations: {iterations}")
    print(f"Total runs: {len(counts) * iterations}")

    # Estimate total time
    total_est = sum(((c / 8) * 3.5 + 150) * iterations for c in counts)
    print(f"Estimated time: ~{int(total_est / 60)} minutes")
    print()

    all_results = {c: [] for c in counts}

    for count in counts:
        print(f"\n{'=' * 60}")
        print(f"  {count}건 x {iterations} iterations")
        print(f"{'=' * 60}")

        for i in range(1, iterations + 1):
            print(f"\n--- [{count}건] Iteration {i}/{iterations} ---")
            result = run_single(count, args.minutes)
            if result:
                all_results[count].append(result)
            else:
                log(f"Iteration {i} FAILED")

    summary = print_summary(all_results)

    # Save raw results
    save_data = {
        "timestamp": datetime.now().isoformat(),
        "config": {"iterations": iterations, "counts": counts},
        "theory": round(8 / 3.163, 4),
        "results": {str(k): v for k, v in all_results.items()},
        "summary": {str(k): v for k, v in summary.items()} if summary else {},
    }
    with open(RESULTS_FILE, "w", encoding="utf-8") as f:
        json.dump(save_data, f, indent=2, ensure_ascii=False)
    print(f"Results saved to {RESULTS_FILE}")


if __name__ == "__main__":
    main()