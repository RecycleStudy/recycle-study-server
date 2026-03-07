ALTER TABLE notification_history
    ADD CONSTRAINT uk_notification_history_review_cycle_id UNIQUE (review_cycle_id);
