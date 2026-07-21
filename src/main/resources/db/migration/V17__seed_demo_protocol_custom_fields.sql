-- Demo snapshots include custom MQTT fields configured per company.
-- Real MQTT payloads overwrite these values as soon as each device reports again.

WITH seed(vehicle_id, fields) AS (
  VALUES
    (1, '{"battery":92,"voltajeBateria":false}'::jsonb),
    (2, '{"battery":88,"voltajeBateria":false}'::jsonb),
    (3, '{"battery":76,"voltajeBateria":true}'::jsonb),
    (7, '{"battery":81,"voltajeBateria":false}'::jsonb),
    (12, '{"battery":92,"voltajeBateria":false}'::jsonb),
    (21, '{"battery":41,"voltajeBateria":true}'::jsonb),
    (101, '{"battery":84}'::jsonb)
)
UPDATE telemetry_snapshots snapshot
SET custom_fields = (seed.fields || COALESCE(NULLIF(snapshot.custom_fields, ''), '{}')::jsonb)::text,
    updated_at = now()
FROM seed
WHERE snapshot.vehicle_id = seed.vehicle_id;

UPDATE telemetry_readings reading
SET custom_fields = ('{"battery":92,"voltajeBateria":false}'::jsonb || COALESCE(NULLIF(reading.custom_fields, ''), '{}')::jsonb)::text
WHERE reading.raw_payload = '{"source":"demo-backfill"}'
  AND reading.vehicle_id IN (1, 2, 3, 7, 12, 21, 101);
