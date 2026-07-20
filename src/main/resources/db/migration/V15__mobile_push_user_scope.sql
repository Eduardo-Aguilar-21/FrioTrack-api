ALTER TABLE mobile_push_notifications ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE INDEX IF NOT EXISTS ix_mobile_push_user_status ON mobile_push_notifications(user_id, status);

ALTER TABLE mobile_push_notifications
    ADD CONSTRAINT fk_mobile_push_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
