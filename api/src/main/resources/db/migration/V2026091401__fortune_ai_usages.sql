CREATE TABLE fortune_ai_usages (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fortune_ai_usages_user_created_at
    ON fortune_ai_usages (user_id, created_at);
