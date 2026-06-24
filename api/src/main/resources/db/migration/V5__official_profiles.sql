CREATE TABLE IF NOT EXISTS public.official_profile_applications (
    id bigserial PRIMARY KEY,
    applicant_id bigint NOT NULL REFERENCES public.users(id),
    type varchar(20) NOT NULL,
    display_name varchar(100) NOT NULL,
    bio text,
    official_url varchar(500),
    contact_email varchar(255) NOT NULL,
    proof_url varchar(500),
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    review_note text,
    profile_id bigint,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.official_profiles (
    id bigserial PRIMARY KEY,
    type varchar(20) NOT NULL,
    display_name varchar(100) NOT NULL,
    slug varchar(120) NOT NULL UNIQUE,
    bio text,
    image_url varchar(500),
    official_url varchar(500),
    instagram_url varchar(500),
    brunch_url varchar(500),
    tumblbug_url varchar(500),
    contact_email varchar(255),
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    verified boolean NOT NULL DEFAULT false,
    featured boolean NOT NULL DEFAULT false,
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT now()
);

ALTER TABLE public.official_profile_applications
    ADD CONSTRAINT fk_official_profile_applications_profile
    FOREIGN KEY (profile_id) REFERENCES public.official_profiles(id);

CREATE TABLE IF NOT EXISTS public.official_profile_members (
    id bigserial PRIMARY KEY,
    profile_id bigint NOT NULL REFERENCES public.official_profiles(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    role varchar(20) NOT NULL DEFAULT 'OWNER',
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_official_profile_members_profile_user UNIQUE (profile_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.official_profile_books (
    id bigserial PRIMARY KEY,
    profile_id bigint NOT NULL REFERENCES public.official_profiles(id),
    book_id bigint NOT NULL REFERENCES public.books(id),
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_official_profile_books_profile_book UNIQUE (profile_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_official_profile_applications_status
    ON public.official_profile_applications (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_official_profiles_slug_active
    ON public.official_profiles (slug)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_official_profile_books_profile
    ON public.official_profile_books (profile_id, created_at DESC);
