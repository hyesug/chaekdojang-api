CREATE TABLE public.contests (
    id bigserial PRIMARY KEY,
    host_profile_id bigint NOT NULL REFERENCES public.official_profiles(id),
    title varchar(200) NOT NULL,
    description text,
    prize_description text,
    entry_type varchar(20) NOT NULL DEFAULT 'REVIEW',
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    submit_start_at timestamp(6) without time zone NOT NULL,
    submit_end_at timestamp(6) without time zone NOT NULL,
    announce_at timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now()
);

CREATE TABLE public.contest_books (
    id bigserial PRIMARY KEY,
    contest_id bigint NOT NULL REFERENCES public.contests(id),
    book_id bigint NOT NULL REFERENCES public.books(id),
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_contest_books_contest_book UNIQUE (contest_id, book_id)
);

CREATE TABLE public.contest_entries (
    id bigserial PRIMARY KEY,
    contest_id bigint NOT NULL REFERENCES public.contests(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    review_id bigint REFERENCES public.reviews(id),
    book_id bigint REFERENCES public.books(id),
    title varchar(200),
    content text,
    status varchar(20) NOT NULL DEFAULT 'SUBMITTED',
    award_rank integer,
    award_name varchar(50),
    submitted_at timestamp(6) without time zone NOT NULL,
    withdrawn_at timestamp(6) without time zone,
    judged_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_contest_entries_contest_user UNIQUE (contest_id, user_id)
);

CREATE INDEX idx_contests_status_submit_end
    ON public.contests (status, submit_end_at DESC);

CREATE INDEX idx_contests_host_created
    ON public.contests (host_profile_id, created_at DESC);

CREATE INDEX idx_contest_entries_contest_submitted
    ON public.contest_entries (contest_id, submitted_at);

CREATE INDEX idx_contest_entries_user_submitted
    ON public.contest_entries (user_id, submitted_at DESC);
