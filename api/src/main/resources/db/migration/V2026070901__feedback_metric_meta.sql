ALTER TABLE metric_events
    ADD COLUMN IF NOT EXISTS meta jsonb;
