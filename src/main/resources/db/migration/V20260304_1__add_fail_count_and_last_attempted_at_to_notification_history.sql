ALTER TABLE notification_history
    ADD COLUMN fail_count         INT      NOT NULL DEFAULT 0,
    ADD COLUMN last_attempted_at  DATETIME NULL;
