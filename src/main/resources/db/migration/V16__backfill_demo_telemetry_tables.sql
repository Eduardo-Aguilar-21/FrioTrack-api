-- Backfill demo telemetry from legacy app_store JSON into relational telemetry tables.

INSERT INTO telemetry_snapshots (vehicle_id, temperature, temperature_state, humidity, door_state, cooling_unit_state, fuel_level, speed, target_range, latitude, longitude, address, last_communication, custom_fields)
VALUES
    (1, '2.1 °C', 'En rango', '48 %', 'Cerrada', 'Encendido', '78 %', '58 km/h', '-2 °C a 5 °C', -11.985, -77.065, 'Panamericana Norte, Los Olivos', 'Hace 1 min', '{}'),
    (2, '3.4 °C', 'En rango', '51 %', 'Cerrada', 'Encendido', '72 %', '54 km/h', '-2 °C a 5 °C', -12.01, -77.115, 'Av. Argentina, Callao', 'Hace 2 min', '{}'),
    (3, '6.7 °C', 'Fuera de rango', '57 %', 'Cerrada', 'Encendido', '61 %', '49 km/h', '-2 °C a 5 °C', -12.027, -77.014, 'Av. Abancay, Lima', 'Hace 2 min', '{}'),
    (7, '4.8 °C', 'En rango', '53 %', 'Abierta', 'Encendido', '66 %', '35 km/h', '-2 °C a 5 °C', -12.071, -76.995, 'Av. Javier Prado Este, San Borja', 'Hace 3 min', '{}'),
    (12, '4.8 °C', 'En rango', '45 %', 'Cerrada', 'Encendido', '65 %', '65 km/h', '-2 °C a 5 °C', -12.0576, -76.9649, 'Ubicacion MQTT', 'Ahora', '{}'),
    (21, NULL, 'Sin datos', NULL, NULL, NULL, NULL, NULL, '-2 °C a 5 °C', -12.115, -77.035, 'Ultima ubicacion: Chorrillos', 'Hace 25 min', '{}'),
    (101, '6.2 °C', 'Fuera de rango', '55 %', 'Cerrada', 'Encendido', '70 %', '46 km/h', '-2 °C a 5 °C', -8.1116, -79.0288, 'Av. Espana, Trujillo', 'Hace 4 min', '{}')
ON CONFLICT (vehicle_id) DO UPDATE SET
    temperature = COALESCE(telemetry_snapshots.temperature, EXCLUDED.temperature),
    temperature_state = COALESCE(telemetry_snapshots.temperature_state, EXCLUDED.temperature_state),
    humidity = COALESCE(NULLIF(telemetry_snapshots.humidity, '--'), EXCLUDED.humidity),
    door_state = COALESCE(telemetry_snapshots.door_state, EXCLUDED.door_state),
    cooling_unit_state = COALESCE(telemetry_snapshots.cooling_unit_state, EXCLUDED.cooling_unit_state),
    fuel_level = COALESCE(NULLIF(telemetry_snapshots.fuel_level, '--'), EXCLUDED.fuel_level),
    speed = COALESCE(NULLIF(telemetry_snapshots.speed, '--'), EXCLUDED.speed),
    target_range = COALESCE(telemetry_snapshots.target_range, EXCLUDED.target_range),
    latitude = COALESCE(telemetry_snapshots.latitude, EXCLUDED.latitude),
    longitude = COALESCE(telemetry_snapshots.longitude, EXCLUDED.longitude),
    address = COALESCE(NULLIF(telemetry_snapshots.address, 'Sin direccion registrada'), EXCLUDED.address),
    last_communication = COALESCE(telemetry_snapshots.last_communication, EXCLUDED.last_communication),
    custom_fields = COALESCE(telemetry_snapshots.custom_fields, EXCLUDED.custom_fields),
    updated_at = now();

WITH seed(vehicle_id, point_time, temperature) AS (
  VALUES
    (1, '06:00', 1.9),
    (1, '07:00', 2.0),
    (1, '08:00', 2.2),
    (1, '09:00', 2.4),
    (1, '10:00', 2.1),
    (1, '11:00', 2.3),
    (1, '12:00', 2.0),
    (1, '13:00', 1.8),
    (1, '14:00', 2.5),
    (1, '15:00', 2.7),
    (1, '16:00', 2.3),
    (1, '17:00', 2.1),
    (2, '06:00', 2.8),
    (2, '07:00', 3.1),
    (2, '08:00', 3.4),
    (2, '09:00', 3.7),
    (2, '10:00', 4.1),
    (2, '11:00', 4.4),
    (2, '12:00', 4.0),
    (2, '13:00', 3.6),
    (2, '14:00', 3.3),
    (2, '15:00', 3.5),
    (2, '16:00', 3.2),
    (2, '17:00', 3.4),
    (3, '06:00', 3.8),
    (3, '07:00', 4.2),
    (3, '08:00', 5.4),
    (3, '09:00', 6.1),
    (3, '10:00', 6.7),
    (3, '11:00', 7.2),
    (3, '12:00', 6.9),
    (3, '13:00', 5.8),
    (3, '14:00', 4.9),
    (3, '15:00', 4.4),
    (3, '16:00', 5.6),
    (3, '17:00', 6.7),
    (7, '06:00', 2.2),
    (7, '07:00', 2.4),
    (7, '08:00', 3.0),
    (7, '09:00', 3.5),
    (7, '10:00', 4.2),
    (7, '11:00', 4.8),
    (7, '12:00', 5.2),
    (7, '13:00', 4.9),
    (7, '14:00', 4.4),
    (7, '15:00', 4.1),
    (7, '16:00', 4.6),
    (7, '17:00', 4.8),
    (12, '06:00', 3.2),
    (12, '08:00', 7.4),
    (12, '10:00', 10.4),
    (12, '12:00', 12.2),
    (12, '14:00', 4.8),
    (12, '15:00', 9.6),
    (12, '16:00', 3.2),
    (12, '17:00', 4.8),
    (101, '06:00', 4.7),
    (101, '07:00', 5.3),
    (101, '08:00', 5.9),
    (101, '09:00', 6.2),
    (101, '10:00', 6.6),
    (101, '11:00', 6.1),
    (101, '12:00', 5.4),
    (101, '13:00', 4.9)
)
INSERT INTO telemetry_readings (vehicle_id, company_id, recorded_at, temperature, humidity, door_state, cooling_unit_state, fuel_level, speed, latitude, longitude, custom_fields, raw_payload)
SELECT v.id, v.company_id, ((current_date::timestamp + seed.point_time::time) AT TIME ZONE 'America/Lima'), seed.temperature, s.humidity, s.door_state, s.cooling_unit_state, s.fuel_level, s.speed, COALESCE(s.latitude, v.latitude), COALESCE(s.longitude, v.longitude), '{}', '{"source":"demo-backfill"}'
FROM seed
JOIN vehicles v ON v.id = seed.vehicle_id
LEFT JOIN telemetry_snapshots s ON s.vehicle_id = v.id
WHERE NOT EXISTS (SELECT 1 FROM telemetry_readings r WHERE r.vehicle_id = v.id AND r.raw_payload = '{"source":"demo-backfill"}');

WITH seed(vehicle_id, type, title, description, event_time, severity) AS (
  VALUES
    (1, 'TEMPERATURE', 'Temperatura estable', 'Lectura dentro de rango: 2.1 °C', '17:00', 'INFO'),
    (1, 'NETWORK', 'Comunicacion correcta', 'Senal GPS y sensor activos', '16:58', 'INFO'),
    (1, 'COOLING', 'Equipo de frio encendido', 'Compresor operando normalmente', '16:45', 'INFO'),
    (2, 'TEMPERATURE', 'Temperatura estable', 'Lectura dentro de rango: 3.4 °C', '17:02', 'INFO'),
    (2, 'DOOR', 'Puerta cerrada', 'Cierre confirmado despues de carga', '16:50', 'INFO'),
    (2, 'COOLING', 'Equipo de frio encendido', 'Compresor manteniendo rango objetivo', '16:35', 'INFO'),
    (2, 'NETWORK', 'Comunicacion correcta', 'Ultimo paquete recibido sin retraso', '16:30', 'INFO'),
    (3, 'TEMPERATURE', 'Temperatura fuera de rango', 'Lectura actual: 6.7 °C', '17:01', 'WARNING'),
    (3, 'TEMPERATURE', 'Tendencia de temperatura alta', 'Tres lecturas consecutivas sobre 5 °C', '16:42', 'WARNING'),
    (3, 'COOLING', 'Equipo de frio encendido', 'Compresor activo durante la desviacion', '16:20', 'INFO'),
    (7, 'DOOR', 'Puerta abierta', 'Apertura detectada en zona de reparto', '17:03', 'WARNING'),
    (7, 'TEMPERATURE', 'Temperatura cerca del limite', 'Lectura actual: 4.8 °C', '17:00', 'WARNING'),
    (7, 'NETWORK', 'Comunicacion correcta', 'Sensor reportando cada minuto', '16:55', 'INFO'),
    (12, 'COOLING', 'Equipo de frio apagado', 'Estado recibido por MQTT', '11:53', 'CRITICAL'),
    (12, 'DOOR', 'Puerta abierta', 'Estado recibido por MQTT', '11:53', 'WARNING'),
    (12, 'TEMPERATURE', 'Temperatura alta: 9.6 °C', 'Fuera de rango', '11:53', 'CRITICAL'),
    (12, 'NETWORK', 'Comunicacion restablecida', 'Senal OK', '10:27', 'INFO'),
    (21, 'NETWORK', 'Sin comunicacion', 'No se reciben paquetes desde hace 25 min', '16:38', 'CRITICAL'),
    (101, 'TEMPERATURE', 'Temperatura fuera de rango', 'Lectura actual: 6.2 °C', '16:56', 'WARNING'),
    (101, 'NETWORK', 'Comunicacion correcta', 'Unidad reportando desde Trujillo', '16:52', 'INFO')
)
INSERT INTO vehicle_events (vehicle_id, type, title, description, occurred_at, severity)
SELECT v.id, seed.type, seed.title, seed.description, ((current_date::timestamp + seed.event_time::time) AT TIME ZONE 'America/Lima'), seed.severity
FROM seed
JOIN vehicles v ON v.id = seed.vehicle_id
WHERE NOT EXISTS (SELECT 1 FROM vehicle_events e WHERE e.vehicle_id = v.id AND e.type = seed.type AND e.title = seed.title AND e.description = seed.description);
