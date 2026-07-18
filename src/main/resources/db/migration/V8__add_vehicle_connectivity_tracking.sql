ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS warning_offline_minutes INTEGER NOT NULL DEFAULT 3;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS critical_offline_minutes INTEGER NOT NULL DEFAULT 10;

ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;

UPDATE companies
SET warning_offline_minutes = COALESCE(warning_offline_minutes, 3),
    critical_offline_minutes = COALESCE(critical_offline_minutes, 10);

UPDATE vehicles
SET last_seen_at = CASE id
    WHEN 1 THEN NOW() - INTERVAL '1 minute'
    WHEN 2 THEN NOW() - INTERVAL '2 minutes'
    WHEN 3 THEN NOW() - INTERVAL '2 minutes'
    WHEN 7 THEN NOW() - INTERVAL '3 minutes'
    WHEN 12 THEN NOW()
    WHEN 21 THEN NOW() - INTERVAL '25 minutes'
    WHEN 101 THEN NOW() - INTERVAL '4 minutes'
    ELSE last_seen_at
END
WHERE last_seen_at IS NULL;
