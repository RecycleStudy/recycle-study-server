"""
Gmail SMTP 응답 시간 측정 스크립트

목적: Toxiproxy에 설정할 지연 시간의 근거 데이터 확보
방법: 실제 Gmail SMTP로 15건 발송하며 각 send() 소요 시간 측정

실행 방법:
  python perftest/measure_smtp_latency.py

환경 변수 (.env 파일에서 읽음):
  GMAIL_USERNAME: Gmail 계정
  GMAIL_PASSWORD: Gmail 앱 비밀번호
"""

import smtplib
import time
import statistics
import os
import sys
from email.mime.text import MIMEText

NUM_EMAILS = 15
SMTP_HOST = "smtp.gmail.com"
SMTP_PORT = 587
SEND_INTERVAL_SEC = 3


def load_env(env_path):
    env = {}
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                key, value = line.split("=", 1)
                env[key.strip()] = value.strip()
    return env


def measure_single_send(host, port, username, password, to_email, index):
    subject = f"[SMTP Latency Test] #{index + 1}"
    body = f"SMTP latency measurement - email {index + 1} of {NUM_EMAILS}"

    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = username
    msg["To"] = to_email

    start = time.time()

    server = smtplib.SMTP(host, port, timeout=30)
    server.starttls()
    server.login(username, password)
    server.send_message(msg)
    server.quit()

    elapsed_ms = (time.time() - start) * 1000
    return elapsed_ms


def main():
    env_path = os.path.join(os.path.dirname(__file__), "..", ".env")

    if not os.path.exists(env_path):
        print(f"ERROR: .env file not found at {env_path}")
        sys.exit(1)

    env = load_env(env_path)
    username = env.get("GMAIL_USERNAME")
    password = env.get("GMAIL_PASSWORD")

    if not username or not password:
        print("ERROR: GMAIL_USERNAME or GMAIL_PASSWORD not found in .env")
        sys.exit(1)

    to_email = username
    results = []

    print(f"Gmail SMTP Latency Measurement")
    print(f"Host: {SMTP_HOST}:{SMTP_PORT}")
    print(f"Emails to send: {NUM_EMAILS}")
    print(f"Sending to: {to_email}")
    print(f"{'=' * 50}")

    for i in range(NUM_EMAILS):
        try:
            elapsed_ms = measure_single_send(
                SMTP_HOST, SMTP_PORT, username, password, to_email, i
            )
            results.append(elapsed_ms)
            print(f"  [{i + 1:2d}/{NUM_EMAILS}] {elapsed_ms:7.0f} ms")
        except Exception as e:
            print(f"  [{i + 1:2d}/{NUM_EMAILS}] FAILED - {e}")

        if i < NUM_EMAILS - 1:
            time.sleep(SEND_INTERVAL_SEC)

    if len(results) < 2:
        print("\nNot enough successful sends to calculate statistics.")
        sys.exit(1)

    print(f"\n{'=' * 50}")
    print(f"Results ({len(results)}/{NUM_EMAILS} successful)")
    print(f"{'=' * 50}")
    print(f"  Average : {statistics.mean(results):7.0f} ms")
    print(f"  Median  : {statistics.median(results):7.0f} ms")
    print(f"  Min     : {min(results):7.0f} ms")
    print(f"  Max     : {max(results):7.0f} ms")
    print(f"  Stdev   : {statistics.stdev(results):7.0f} ms")

    sorted_results = sorted(results)
    p95_index = int(len(sorted_results) * 0.95)
    print(f"  P95     : {sorted_results[min(p95_index, len(sorted_results) - 1)]:7.0f} ms")

    print(f"\n--> Toxiproxy latency 권장값: {int(statistics.median(results))} ms")
    print(f"--> Toxiproxy jitter 권장값 : {int(statistics.stdev(results))} ms")


if __name__ == "__main__":
    main()
