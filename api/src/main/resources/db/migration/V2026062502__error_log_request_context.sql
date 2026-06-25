ALTER TABLE error_logs
    ADD COLUMN IF NOT EXISTS user_agent varchar(500),
    ADD COLUMN IF NOT EXISTS referer varchar(500);
