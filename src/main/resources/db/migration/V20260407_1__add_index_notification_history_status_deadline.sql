CREATE INDEX idx_nh_status_deadline
    ON notification_history (status, deadline);
