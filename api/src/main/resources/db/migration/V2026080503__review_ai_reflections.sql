CREATE TABLE review_follow_up_questions (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE REFERENCES reviews(id) ON DELETE CASCADE,
    question VARCHAR(600) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'AI',
    source_hash VARCHAR(64) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_follow_up_source_check CHECK (source IN ('AI', 'USER_EDITED'))
);

CREATE TABLE review_change_comparisons (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL UNIQUE REFERENCES reviews(id) ON DELETE CASCADE,
    previous_review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    previous_focus VARCHAR(800) NOT NULL,
    current_focus VARCHAR(800) NOT NULL,
    shared_thought VARCHAR(800) NOT NULL,
    changed_perspective VARCHAR(800) NOT NULL,
    new_element VARCHAR(800) NOT NULL,
    reflection_question VARCHAR(600) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'AI',
    source_hash VARCHAR(64) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_change_comparison_source_check CHECK (source IN ('AI', 'USER_EDITED'))
);

CREATE INDEX idx_review_change_comparisons_previous
    ON review_change_comparisons(previous_review_id);
