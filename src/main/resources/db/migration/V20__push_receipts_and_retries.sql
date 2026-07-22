ALTER TABLE mobile_push_notifications ADD COLUMN IF NOT EXISTS receipt_checked_at TIMESTAMPTZ;
ALTER TABLE mobile_push_notifications ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS ix_mobile_push_receipt_pending ON mobile_push_notifications(status, receipt_checked_at) WHERE ticket_id IS NOT NULL;
