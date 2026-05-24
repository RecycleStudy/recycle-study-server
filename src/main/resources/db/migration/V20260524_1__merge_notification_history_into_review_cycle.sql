-- 1. review_cycle에 컬럼 추가 (NULL 허용으로 시작, 데이터 이전 후 제약 추가)
ALTER TABLE review_cycle
    ADD COLUMN status            ENUM('FAILED', 'PENDING', 'SENT') NULL,
    ADD COLUMN fail_count        INT                                NULL,
    ADD COLUMN last_attempted_at DATETIME                          NULL,
    ADD COLUMN deadline          DATETIME                          NULL;

-- 2. notification_history → review_cycle 데이터 이전
UPDATE review_cycle rc
    INNER JOIN notification_history nh ON nh.review_cycle_id = rc.id
SET rc.status            = nh.status,
    rc.fail_count        = nh.fail_count,
    rc.last_attempted_at = nh.last_attempted_at,
    rc.deadline          = nh.deadline;

-- 3. NOT NULL 제약 추가
ALTER TABLE review_cycle
    MODIFY COLUMN status     ENUM('FAILED', 'PENDING', 'SENT') NOT NULL,
    MODIFY COLUMN fail_count INT                                NOT NULL DEFAULT 0,
    MODIFY COLUMN deadline   DATETIME                          NOT NULL;

-- 4. 기존 단일 인덱스 삭제
DROP INDEX idx_review_cycle_scheduled_at ON review_cycle;

-- 5. 최적 복합 인덱스 추가
--    이메일 발송 스케줄러: status = PENDING (동등) + scheduled_at <= ? (범위)
CREATE INDEX idx_rc_status_scheduled_at ON review_cycle (status, scheduled_at);
--    재시도 스케줄러: status = FAILED (동등) + deadline > ? (범위)
CREATE INDEX idx_rc_status_deadline ON review_cycle (status, deadline);

-- ※ notification_history 테이블 DROP은 마이그레이션에 포함하지 않음.
--   데이터 검증 완료 후 별도로 직접 수행할 예정.
