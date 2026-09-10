ALTER TABLE public.notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'LIKE', 'COMMENT', 'FOLLOW', 'SAME_BOOK_REVIEW',
        'GROUP_JOIN_REQUEST', 'GROUP_JOINED', 'GROUP_JOIN_APPROVED',
        'REVIEW_CONTINUED',
        'CAMPAIGN_SELECTED', 'CAMPAIGN_REJECTED', 'CAMPAIGN_INVITED',
        'CONTEST_AWARDED', 'CONTEST_NOT_AWARDED'
    ));
