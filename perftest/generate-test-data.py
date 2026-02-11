"""
Performance test data generation/cleanup script

Usage:
  python perftest/generate-test-data.py scheduler --count 500
  python perftest/generate-test-data.py scheduler --count 500 --minutes 3
  python perftest/generate-test-data.py api --count 100
  python perftest/generate-test-data.py cleanup
  python perftest/generate-test-data.py status

Commands:
  scheduler  - ReviewCycle N건을 특정 scheduledAt으로 삽입 (스케줄러 부하 테스트용)
               member -> review -> review_cycle -> notification_history
               device는 스케줄러 흐름에 불필요하므로 생성하지 않음
  api        - Member + Device N건 생성, k6에서 사용할 device ID JSON 출력
               device는 API 인증(X-device-Id)에만 필요
  cleanup    - perftest_ 접두사 테스트 데이터 전체 삭제
  status     - 현재 테스트 데이터 건수 확인
"""

import subprocess
import sys
import json
import argparse

MYSQL_CONTAINER = "perftest-mysql"
MYSQL_USER = "perftest"
MYSQL_PASSWORD = "perftest"
MYSQL_DB = "recycle_study"
TEST_EMAIL_PREFIX = "perftest_"
TEST_DEVICE_PREFIX = "perftest_device_"
BATCH_SIZE = 500


def run_sql(sql):
    result = subprocess.run(
        [
            "docker", "exec", "-i", MYSQL_CONTAINER, "mysql",
            f"-u{MYSQL_USER}", f"-p{MYSQL_PASSWORD}", MYSQL_DB,
            "-N", "-B", "--default-character-set=utf8mb4",
        ],
        input=sql,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    stderr = result.stderr.replace(
        "mysql: [Warning] Using a password on the command line interface can be insecure.\n", ""
    ).strip()
    if result.returncode != 0:
        print(f"SQL Error: {stderr}")
        sys.exit(1)
    if stderr:
        print(f"SQL Warning: {stderr}", file=sys.stderr)
    return result.stdout.strip()


def insert_batch(table, columns, values_list):
    for i in range(0, len(values_list), BATCH_SIZE):
        batch = values_list[i:i + BATCH_SIZE]
        values_str = ",\n".join(batch)
        run_sql(f"INSERT INTO {table} ({columns}) VALUES {values_str};")


def parse_id_rows(output):
    return [int(x) for x in output.split("\n") if x.strip()]


def generate_scheduler_data(count, minutes_ahead):
    print(f"Scheduler test data generation: {count} users")
    print(f"  scheduledAt = NOW + {minutes_ahead} minutes (KST)")
    print("")

    target = run_sql(f"""
        SELECT DATE_FORMAT(
            CONVERT_TZ(
                DATE_ADD(NOW(), INTERVAL {minutes_ahead} MINUTE),
                'UTC', 'Asia/Seoul'
            ),
            '%Y-%m-%d %H:%i:00.000000'
        );
    """)
    now_kst = run_sql("""
        SELECT DATE_FORMAT(
            CONVERT_TZ(NOW(), 'UTC', 'Asia/Seoul'),
            '%Y-%m-%d %H:%i:%s'
        );
    """)
    print(f"  Current time (KST): {now_kst}")
    print(f"  Target scheduledAt: {target}")
    print("")

    # 1. Members
    print(f"  [1/4] Inserting {count} members...")
    member_values = [
        f"('{TEST_EMAIL_PREFIX}{i}@test.com', NOW(), NULL, NULL)"
        for i in range(1, count + 1)
    ]
    insert_batch("member", "email, created_at, modified_at, notification_time", member_values)

    member_ids = parse_id_rows(run_sql(f"""
        SELECT id FROM member
        WHERE email LIKE '{TEST_EMAIL_PREFIX}%'
        ORDER BY id;
    """))
    actual_count = min(count, len(member_ids))
    if len(member_ids) < count:
        print(f"  Warning: {len(member_ids)} members found (expected {count})")

    # 2. Reviews (1 per member)
    print(f"  [2/4] Inserting {actual_count} reviews...")
    review_values = [
        f"('https://perftest.example.com/article/{i}', {member_ids[i-1]}, NOW(), NULL)"
        for i in range(1, actual_count + 1)
    ]
    insert_batch("review", "url, member_id, created_at, modified_at", review_values)

    review_ids = parse_id_rows(run_sql(f"""
        SELECT r.id FROM review r
        JOIN member m ON r.member_id = m.id
        WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%'
        ORDER BY r.id;
    """))

    # 3. ReviewCycles (1 per review, all same scheduledAt)
    print(f"  [3/4] Inserting {len(review_ids)} review_cycles...")
    cycle_values = [
        f"({rid}, '{target}', NOW(), NULL)"
        for rid in review_ids
    ]
    insert_batch("review_cycle", "review_id, scheduled_at, created_at, modified_at", cycle_values)

    cycle_ids = parse_id_rows(run_sql(f"""
        SELECT rc.id FROM review_cycle rc
        JOIN review r ON rc.review_id = r.id
        JOIN member m ON r.member_id = m.id
        WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%'
        ORDER BY rc.id;
    """))

    # 4. NotificationHistories (PENDING)
    print(f"  [4/4] Inserting {len(cycle_ids)} notification_histories (PENDING)...")
    nh_values = [
        f"({cid}, 'PENDING', NOW(), NULL)"
        for cid in cycle_ids
    ]
    insert_batch("notification_history", "review_cycle_id, status, created_at, modified_at", nh_values)

    print("")
    print(f"Done. {actual_count} test datasets created.")
    print(f"Scheduler will process at: {target} (KST)")
    print(f"Expected: {actual_count} emails (1 per user, 1 URL each)")


def generate_api_data(count):
    print(f"API test data generation: {count} users with active devices")
    print("")

    # 1. Members
    print(f"  [1/2] Inserting {count} members...")
    member_values = [
        f"('{TEST_EMAIL_PREFIX}{i}@test.com', NOW(), NULL, NULL)"
        for i in range(1, count + 1)
    ]
    insert_batch("member", "email, created_at, modified_at, notification_time", member_values)

    member_ids = parse_id_rows(run_sql(f"""
        SELECT id FROM member
        WHERE email LIKE '{TEST_EMAIL_PREFIX}%'
        ORDER BY id;
    """))
    actual_count = min(count, len(member_ids))

    # 2. Active devices (API 인증용)
    print(f"  [2/2] Inserting {actual_count} active devices...")
    device_values = [
        f"('{TEST_DEVICE_PREFIX}{i}', 1, '2099-12-31 23:59:59.000000', {member_ids[i-1]}, NOW(), NULL)"
        for i in range(1, actual_count + 1)
    ]
    insert_batch(
        "device",
        "identifier, is_active, activation_expires_at, member_id, created_at, modified_at",
        device_values,
    )

    # 3. Output device IDs for k6
    devices = [
        {"email": f"{TEST_EMAIL_PREFIX}{i}@test.com", "deviceId": f"{TEST_DEVICE_PREFIX}{i}"}
        for i in range(1, actual_count + 1)
    ]
    output_path = "perftest/k6/users.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(devices, f, indent=2)

    print("")
    print(f"Done. {actual_count} users with active devices created.")
    print(f"Device IDs: {output_path}")


def cleanup():
    print("Cleaning up perftest data...")

    steps = [
        ("notification_history", f"""
            DELETE nh FROM notification_history nh
            JOIN review_cycle rc ON nh.review_cycle_id = rc.id
            JOIN review r ON rc.review_id = r.id
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("review_cycle", f"""
            DELETE rc FROM review_cycle rc
            JOIN review r ON rc.review_id = r.id
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("review", f"""
            DELETE r FROM review r
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("device", f"""
            DELETE d FROM device d
            JOIN member m ON d.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("member", f"""
            DELETE FROM member WHERE email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
    ]

    for table, sql in steps:
        affected = run_sql(f"{sql} SELECT ROW_COUNT();")
        print(f"  {table}: {affected} rows deleted")

    print("")
    print("Done. MailPit inbox 초기화는 별도 실행:")
    print("  curl -X DELETE http://localhost:8025/api/v1/messages")


def status():
    print("Perftest data status:")
    print("")

    tables = [
        ("member", f"SELECT COUNT(*) FROM member WHERE email LIKE '{TEST_EMAIL_PREFIX}%';"),
        ("device", f"""
            SELECT COUNT(*) FROM device d
            JOIN member m ON d.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("review", f"""
            SELECT COUNT(*) FROM review r
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("review_cycle", f"""
            SELECT COUNT(*) FROM review_cycle rc
            JOIN review r ON rc.review_id = r.id
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
        ("notification_history", f"""
            SELECT COUNT(*) FROM notification_history nh
            JOIN review_cycle rc ON nh.review_cycle_id = rc.id
            JOIN review r ON rc.review_id = r.id
            JOIN member m ON r.member_id = m.id
            WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%';
        """),
    ]

    for table, sql in tables:
        count = run_sql(sql)
        print(f"  {table}: {count}")

    nh_status = run_sql(f"""
        SELECT nh.status, COUNT(*)
        FROM notification_history nh
        JOIN review_cycle rc ON nh.review_cycle_id = rc.id
        JOIN review r ON rc.review_id = r.id
        JOIN member m ON r.member_id = m.id
        WHERE m.email LIKE '{TEST_EMAIL_PREFIX}%'
        GROUP BY nh.status;
    """)
    if nh_status:
        print("")
        print("  notification_history by status:")
        for line in nh_status.split("\n"):
            if line.strip():
                parts = line.split("\t")
                print(f"    {parts[0]}: {parts[1]}")


def main():
    parser = argparse.ArgumentParser(description="Performance test data generator")
    subparsers = parser.add_subparsers(dest="command", help="Command")

    sp = subparsers.add_parser("scheduler", help="Generate scheduler test data")
    sp.add_argument("--count", type=int, required=True, help="Number of users (= emails)")
    sp.add_argument("--minutes", type=int, default=2, help="Minutes ahead for scheduledAt (default: 2)")

    ap = subparsers.add_parser("api", help="Generate API test data")
    ap.add_argument("--count", type=int, required=True, help="Number of users")

    subparsers.add_parser("cleanup", help="Remove all perftest data")
    subparsers.add_parser("status", help="Show perftest data counts")

    args = parser.parse_args()

    if args.command == "scheduler":
        generate_scheduler_data(args.count, args.minutes)
    elif args.command == "api":
        generate_api_data(args.count)
    elif args.command == "cleanup":
        cleanup()
    elif args.command == "status":
        status()
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
