ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS target_slug VARCHAR(120);
