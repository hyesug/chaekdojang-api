CREATE TABLE IF NOT EXISTS reading_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_year INTEGER NOT NULL,
    target_count INTEGER NOT NULL,
    public_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reading_goals_user_year UNIQUE (user_id, goal_year),
    CONSTRAINT chk_reading_goals_target_count CHECK (target_count BETWEEN 1 AND 999)
);

CREATE INDEX IF NOT EXISTS idx_reading_goals_user_year ON reading_goals(user_id, goal_year);
