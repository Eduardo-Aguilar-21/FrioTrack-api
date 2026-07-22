ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS detected_protocol VARCHAR(80);
