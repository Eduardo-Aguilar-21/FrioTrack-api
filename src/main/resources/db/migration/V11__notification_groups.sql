CREATE TABLE notification_groups (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(160) NOT NULL,
    description TEXT,
    alert_types VARCHAR(500) NOT NULL,
    severities VARCHAR(300) NOT NULL,
    channels VARCHAR(300) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_notification_groups_company_name UNIQUE (company_id, name)
);

CREATE TABLE notification_group_users (
    group_id BIGINT NOT NULL REFERENCES notification_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE notification_group_vehicles (
    group_id BIGINT NOT NULL REFERENCES notification_groups(id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    PRIMARY KEY (group_id, vehicle_id)
);

CREATE INDEX ix_notification_groups_company ON notification_groups(company_id);
CREATE INDEX ix_notification_group_users_user ON notification_group_users(user_id);
CREATE INDEX ix_notification_group_vehicles_vehicle ON notification_group_vehicles(vehicle_id);

INSERT INTO notification_groups (company_id, name, description, alert_types, severities, channels, status)
VALUES
    (1, 'Operaciones Frio', 'Alertas operativas de temperatura, puerta y equipo de frio.', 'TEMPERATURE,DOOR,COOLING_UNIT,OFFLINE', 'CRITICAL,WARNING', 'APP,EMAIL', 'ACTIVE'),
    (2, 'Supervision Norte', 'Alertas principales para la operacion norte.', 'TEMPERATURE,OFFLINE', 'CRITICAL,WARNING', 'APP', 'ACTIVE')
ON CONFLICT (company_id, name) DO NOTHING;

INSERT INTO notification_group_users (group_id, user_id)
SELECT ng.id, u.id
FROM notification_groups ng
JOIN users u ON u.company_id = ng.company_id
WHERE (ng.company_id = 1 AND u.username IN ('admin', 'operador'))
   OR (ng.company_id = 2 AND u.username IN ('admin-norte'))
ON CONFLICT DO NOTHING;

INSERT INTO notification_group_vehicles (group_id, vehicle_id)
SELECT ng.id, v.id
FROM notification_groups ng
JOIN vehicles v ON v.company_id = ng.company_id
WHERE (ng.company_id = 1 AND v.id IN (1, 12, 21))
   OR (ng.company_id = 2 AND v.id IN (101))
ON CONFLICT DO NOTHING;
