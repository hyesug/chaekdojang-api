CREATE TABLE reading_group_questions (
    id BIGSERIAL PRIMARY KEY,
    group_book_id BIGINT NOT NULL REFERENCES reading_group_books(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question VARCHAR(1000) NOT NULL,
    ai_suggested BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    model VARCHAR(100),
    prompt_version VARCHAR(60),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reading_group_questions_book_created
    ON reading_group_questions(group_book_id, created_at ASC);

CREATE TABLE reading_group_question_responses (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES reading_group_questions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reading_group_question_response UNIQUE (question_id, user_id)
);

CREATE INDEX idx_reading_group_question_responses_question_created
    ON reading_group_question_responses(question_id, created_at ASC);
