ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('LIKE', 'COMMENT', 'FOLLOW', 'SAME_BOOK_REVIEW', 'GROUP_JOIN_REQUEST', 'GROUP_JOINED'));
