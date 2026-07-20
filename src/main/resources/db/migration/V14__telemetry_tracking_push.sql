CREATE TABLE telemetry_snapshots (
    vehicle_id BIGINT PRIMARY KEY REFERENCES vehicles(id) ON DELETE CASCADE,
    temperature VARCHAR(60),
    temperature_state VARCHAR(120),
    humidity VARCHAR(60),
    door_state VARCHAR(120),
    cooling_unit_state VARCHAR(120),
    fuel_level VARCHAR(60),
    speed VARCHAR(60),
    target_range VARCHAR(120),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    address VARCHAR(255),
    last_communication VARCHAR(120),
    custom_fields TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE telemetry_readings (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    temperature DOUBLE PRECISION,
    humidity VARCHAR(60),
    door_state VARCHAR(120),
    cooling_unit_state VARCHAR(120),
    fuel_level VARCHAR(60),
    speed VARCHAR(60),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    custom_fields TEXT,
    raw_payload TEXT
);

CREATE INDEX ix_telemetry_readings_vehicle_time ON telemetry_readings(vehicle_id, recorded_at);
CREATE INDEX ix_telemetry_readings_company_time ON telemetry_readings(company_id, recorded_at);

CREATE TABLE vehicle_events (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(220) NOT NULL,
    description TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    severity VARCHAR(40) NOT NULL
);

CREATE INDEX ix_vehicle_events_vehicle_time ON vehicle_events(vehicle_id, occurred_at);

CREATE TABLE mobile_push_notifications (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    mobile_token VARCHAR(160) NOT NULL,
    push_token VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    ticket_id VARCHAR(160),
    failure_reason TEXT,
    sent_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_mobile_push_once UNIQUE (alert_id, mobile_token)
);

CREATE INDEX ix_mobile_push_company_status ON mobile_push_notifications(company_id, status);
CREATE INDEX ix_mobile_push_alert ON mobile_push_notifications(alert_id);
