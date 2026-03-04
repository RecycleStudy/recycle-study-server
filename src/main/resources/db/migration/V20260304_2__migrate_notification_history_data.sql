-- 기존 append-only 레코드를 review_cycle_id당 1개로 통합

-- 1. PENDING 레코드(기준 레코드)에 fail_count 반영
--    review_cycle_id별 FAILED 개수를 집계해서 PENDING 레코드에 update
UPDATE notification_history nh
JOIN (
    SELECT review_cycle_id, COUNT(*) AS cnt
    FROM notification_history
    WHERE status = 'FAILED'
    GROUP BY review_cycle_id
) sub ON nh.review_cycle_id = sub.review_cycle_id
SET nh.fail_count = sub.cnt
WHERE nh.status = 'PENDING';

-- 2. SENT가 있는 경우: PENDING → SENT 로 status 변경
UPDATE notification_history nh
JOIN (
    SELECT DISTINCT review_cycle_id
    FROM notification_history
    WHERE status = 'SENT'
) sub ON nh.review_cycle_id = sub.review_cycle_id
SET nh.status = 'SENT'
WHERE nh.status = 'PENDING';

-- 3. SENT 없고 FAILED 있는 경우: PENDING → FAILED 로 status 변경
UPDATE notification_history nh
JOIN (
    SELECT review_cycle_id, COUNT(*) AS cnt
    FROM notification_history
    WHERE status = 'FAILED'
    GROUP BY review_cycle_id
) sub ON nh.review_cycle_id = sub.review_cycle_id
LEFT JOIN (
    SELECT DISTINCT review_cycle_id
    FROM notification_history
    WHERE status = 'SENT'
) sent ON nh.review_cycle_id = sent.review_cycle_id
SET nh.status = 'FAILED'
WHERE nh.status = 'PENDING'
  AND sent.review_cycle_id IS NULL;

-- 4. review_cycle_id당 MIN(id) 레코드 1개만 남기고 나머지 삭제
--    PENDING이 항상 먼저 INSERT되므로 MIN(id) = 기준 레코드
DELETE FROM notification_history
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM notification_history
        GROUP BY review_cycle_id
    ) AS keeper
);
