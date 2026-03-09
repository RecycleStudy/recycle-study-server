-- review_cycle.scheduled_at 보정: PENDING 상태 + 미래 주기만 (-9h)
UPDATE review_cycle rc
    INNER JOIN notification_history nh ON nh.review_cycle_id = rc.id
SET rc.scheduled_at = DATE_SUB(rc.scheduled_at, INTERVAL 9 HOUR)
WHERE nh.status = 'PENDING'
  AND rc.scheduled_at > NOW();

-- notification_history.deadline 보정: PENDING 상태만 (-9h)
UPDATE notification_history nh
SET nh.deadline = DATE_SUB(nh.deadline, INTERVAL 9 HOUR)
WHERE nh.status = 'PENDING';

-- member.notification_time 보정: Seoul 기준 -> UTC (-9h)
-- LocalTime은 날짜가 없으므로 자정을 넘는 케이스 처리 필요
-- 예: 01:00 Seoul -> UTC 전날 16:00 → LocalTime으로 16:00 저장
UPDATE member
SET notification_time = CASE
    WHEN notification_time >= '09:00:00'
        THEN SUBTIME(notification_time, '09:00:00')
    ELSE
        ADDTIME(notification_time, '15:00:00')
END
WHERE notification_time IS NOT NULL;
