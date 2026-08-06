ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS previous_review_id BIGINT;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_previous_review
    FOREIGN KEY (previous_review_id) REFERENCES reviews(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_active_previous_review
    ON reviews(previous_review_id)
    WHERE previous_review_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_author_book_created_at
    ON reviews(author_id, book_id, created_at);
