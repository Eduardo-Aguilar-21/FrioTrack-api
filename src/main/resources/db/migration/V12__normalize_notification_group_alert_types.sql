UPDATE notification_groups
SET alert_types = replace(alert_types, 'COOLING_UNIT', 'COOLING')
WHERE alert_types LIKE '%COOLING_UNIT%';
