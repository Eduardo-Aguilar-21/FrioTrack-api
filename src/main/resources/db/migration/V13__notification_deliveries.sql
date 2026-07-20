CREATE TABLE notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES notification_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_notification_delivery_once UNIQUE (alert_id, user_id, channel)
);

CREATE INDEX ix_notification_deliveries_alert ON notification_deliveries(alert_id);
CREATE INDEX ix_notification_deliveries_user ON notification_deliveries(user_id);
CREATE INDEX ix_notification_deliveries_status ON notification_deliveries(status);
