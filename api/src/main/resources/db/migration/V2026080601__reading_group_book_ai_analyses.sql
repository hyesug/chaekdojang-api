CREATE TABLE reading_group_book_ai_analyses (
    id BIGSERIAL PRIMARY KEY,
    group_book_id BIGINT NOT NULL UNIQUE REFERENCES reading_group_books(id) ON DELETE CASCADE,
    generated_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    analysis_json TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    review_count INTEGER NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(60) NOT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reading_group_book_ai_analyses_updated
    ON reading_group_book_ai_analyses(updated_at DESC);
