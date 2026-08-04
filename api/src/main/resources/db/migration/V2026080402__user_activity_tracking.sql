ALTER TABLE metric_events
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(80),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS browser VARCHAR(80),
    ADD COLUMN IF NOT EXISTS operating_system VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_metric_events_device_id ON metric_events(device_id);
CREATE INDEX IF NOT EXISTS idx_metric_events_ip_created_at ON metric_events(ip, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_metric_events_user_created_at ON metric_events(user_id, created_at DESC);

ALTER TABLE access_logs
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(80);

ALTER TABLE access_logs
    DROP CONSTRAINT IF EXISTS fk_access_logs_user;

ALTER TABLE access_logs
    ADD CONSTRAINT fk_access_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_access_logs_user_created_at ON access_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_logs_ip_created_at ON access_logs(ip, created_at DESC);
