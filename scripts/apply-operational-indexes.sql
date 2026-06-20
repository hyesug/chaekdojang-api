-- Run this with the owner of the existing production tables.
-- The application DB user may not be able to create indexes on tables it does not own.

CREATE INDEX IF NOT EXISTS idx_access_logs_created_at
    ON public.access_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_access_logs_method_status_created_at
    ON public.access_logs (method, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_error_logs_created_at
    ON public.error_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_error_logs_level_status_created_at
    ON public.error_logs (level, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_feed
    ON public.reviews (hidden, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_author_created_at
    ON public.reviews (author_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_book_created_at
    ON public.reviews (book_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_comments_review_created_at
    ON public.comments (review_id, created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_follows_following_created_at
    ON public.follows (following_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_follows_follower_created_at
    ON public.follows (follower_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_receiver_created_at
    ON public.notifications (receiver_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_receiver_unread
    ON public.notifications (receiver_id)
    WHERE is_read = false;

CREATE INDEX IF NOT EXISTS idx_libraries_user_updated_at
    ON public.libraries (user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_libraries_user_status_updated_at
    ON public.libraries (user_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_books_title
    ON public.books (title);

CREATE INDEX IF NOT EXISTS idx_books_author
    ON public.books (author);
