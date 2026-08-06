CREATE TABLE reading_recap_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    frequency VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    include_following BOOLEAN NOT NULL DEFAULT TRUE,
    include_memories BOOLEAN NOT NULL DEFAULT TRUE,
    last_delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reading_recap_frequency_check CHECK (frequency IN ('WEEKLY', 'MONTHLY'))
);

CREATE TABLE reading_recap_issues (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_key VARCHAR(40) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    title VARCHAR(120) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    own_review_count INTEGER NOT NULL DEFAULT 0,
    following_review_count INTEGER NOT NULL DEFAULT 0,
    saved_review_count INTEGER NOT NULL DEFAULT 0,
    continued_review_count INTEGER NOT NULL DEFAULT 0,
    featured_review_id BIGINT REFERENCES reviews(id) ON DELETE SET NULL,
    memory_review_id BIGINT REFERENCES reviews(id) ON DELETE SET NULL,
    memory_book_title VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT uk_reading_recap_issue_period UNIQUE (user_id, period_key)
);

CREATE INDEX idx_reading_recap_issues_user_created
    ON reading_recap_issues(user_id, created_at DESC);
