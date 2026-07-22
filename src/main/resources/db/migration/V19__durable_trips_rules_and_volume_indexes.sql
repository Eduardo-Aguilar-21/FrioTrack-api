CREATE TABLE tracked_trips (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(160) NOT NULL UNIQUE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    start_latitude DOUBLE PRECISION,
    start_longitude DOUBLE PRECISION,
    end_latitude DOUBLE PRECISION,
    end_longitude DOUBLE PRECISION,
    distance_km DOUBLE PRECISION NOT NULL DEFAULT 0,
    duration_seconds BIGINT NOT NULL DEFAULT 0,
    stop_count INTEGER NOT NULL DEFAULT 0,
    sensor_data JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_tracked_trips_vehicle_started ON tracked_trips(vehicle_id, started_at DESC);
CREATE INDEX ix_tracked_trips_company_started ON tracked_trips(company_id, started_at DESC);
CREATE INDEX ix_tracked_trips_status_updated ON tracked_trips(status, updated_at);

CREATE TABLE advanced_rule_states (
    state_key VARCHAR(220) PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    protocol VARCHAR(60) NOT NULL,
    rule_type VARCHAR(140) NOT NULL,
    condition_since TIMESTAMPTZ,
    stopped_since TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_advanced_rule_states_vehicle ON advanced_rule_states(vehicle_id);
CREATE INDEX ix_advanced_rule_states_updated ON advanced_rule_states(updated_at);

CREATE INDEX IF NOT EXISTS ix_alerts_company_id_desc ON alerts(company_id, id DESC);
CREATE INDEX IF NOT EXISTS ix_alerts_company_status_id ON alerts(company_id, status, id DESC);
CREATE INDEX IF NOT EXISTS ix_vehicle_events_occurred ON vehicle_events(occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_mobile_push_status_created ON mobile_push_notifications(status, created_at);
