ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS source_review_id BIGINT;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_source_review
    FOREIGN KEY (source_review_id) REFERENCES reviews(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_source_review_created_at
    ON reviews(source_review_id, created_at DESC)
    WHERE source_review_id IS NOT NULL AND deleted_at IS NULL;

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'LIKE', 'COMMENT', 'FOLLOW', 'SAME_BOOK_REVIEW',
        'GROUP_JOIN_REQUEST', 'GROUP_JOINED', 'GROUP_JOIN_APPROVED',
        'REVIEW_CONTINUED'
    ));
