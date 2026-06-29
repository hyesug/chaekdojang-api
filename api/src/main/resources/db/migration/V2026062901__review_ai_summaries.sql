CREATE TABLE review_ai_summaries (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE REFERENCES reviews(id) ON DELETE CASCADE,
    one_line_review VARCHAR(60),
    recommended_for VARCHAR(120),
    impressive_point VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    summary_source VARCHAR(20) NOT NULL DEFAULT 'AI',
    user_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE review_ai_summary_emotion_keywords (
    summary_id BIGINT NOT NULL REFERENCES review_ai_summaries(id) ON DELETE CASCADE,
    keyword VARCHAR(50) NOT NULL
);

CREATE TABLE review_ai_summary_jobs (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_review_ai_summary_jobs_status_id ON review_ai_summary_jobs(status, id);
CREATE INDEX idx_review_ai_summary_jobs_review_id ON review_ai_summary_jobs(review_id);
