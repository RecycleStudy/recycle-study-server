-- Step 1: 다음 주기가 있는 레코드 -> deadline = 다음 주기의 scheduled_at
UPDATE notification_history nh
    INNER JOIN review_cycle rc ON rc.id = nh.review_cycle_id
    INNER JOIN (
        SELECT rc1.id AS current_id, MIN(rc2.scheduled_at) AS next_scheduled_at
        FROM review_cycle rc1
            INNER JOIN review_cycle rc2
                ON rc2.review_id = rc1.review_id
                AND rc2.scheduled_at > rc1.scheduled_at
        GROUP BY rc1.id
    ) next ON next.current_id = rc.id
SET nh.deadline = next.next_scheduled_at
WHERE nh.deadline IS NULL;

-- Step 2: 마지막 주기 -> deadline = scheduled_at + 24시간
UPDATE notification_history nh
    INNER JOIN review_cycle rc ON rc.id = nh.review_cycle_id
SET nh.deadline = rc.scheduled_at + INTERVAL 24 HOUR
WHERE nh.deadline IS NULL;

-- Step 3: NOT NULL 제약 추가
ALTER TABLE notification_history
    MODIFY COLUMN deadline DATETIME NOT NULL;
