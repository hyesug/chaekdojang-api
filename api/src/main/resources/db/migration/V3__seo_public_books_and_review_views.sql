ALTER TABLE public.books
    ADD COLUMN IF NOT EXISTS slug character varying(160),
    ADD COLUMN IF NOT EXISTS description text,
    ADD COLUMN IF NOT EXISTS published_year integer,
    ADD COLUMN IF NOT EXISTS seo_title character varying(200),
    ADD COLUMN IF NOT EXISTS seo_description character varying(500),
    ADD COLUMN IF NOT EXISTS is_public boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone NOT NULL DEFAULT now();

ALTER TABLE public.reviews
    ADD COLUMN IF NOT EXISTS view_count bigint NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_books_slug_public
    ON public.books (slug)
    WHERE deleted_at IS NULL AND is_public = true;

CREATE INDEX IF NOT EXISTS idx_books_public_updated_at
    ON public.books (is_public, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_book_visible_created_at
    ON public.reviews (book_id, hidden, deleted_at, created_at DESC);

UPDATE public.books
SET slug = CASE
        WHEN title ILIKE '%데미안%' THEN 'demian'
        WHEN title ILIKE '%인간실격%' OR title ILIKE '%인간 실격%' THEN 'human-disqualification'
        WHEN title ILIKE '%이방인%' THEN 'the-stranger'
        WHEN title ILIKE '%싯다르타%' THEN 'siddhartha'
        WHEN title ILIKE '%스토너%' THEN 'stoner'
        WHEN title ILIKE '%불편한 편의점%' THEN 'inconvenient-convenience-store'
        WHEN title ILIKE '%모순%' THEN 'mosun'
        WHEN title ILIKE '%긴긴밤%' THEN 'the-long-long-night'
        WHEN title ILIKE '%소년이 온다%' THEN 'human-acts'
        WHEN title ILIKE '%물고기는 존재하지 않는다%' THEN 'why-fish-dont-exist'
        ELSE slug
    END
WHERE slug IS NULL;

UPDATE public.books
SET description = title || '은(는) ' || author || '의 책입니다. 책도장에서 이 책을 읽은 사람들의 독후감, 리뷰, 독서 기록과 인상 깊은 문장을 확인해보세요.'
WHERE description IS NULL;

UPDATE public.books
SET seo_title = title || ' 독후감과 문장 기록 | 책도장'
WHERE seo_title IS NULL;

UPDATE public.books
SET seo_description = author || '의 ' || title || '을 읽고 남긴 독후감, 인상 깊은 문장, 독서 기록을 책도장에서 확인해보세요.'
WHERE seo_description IS NULL;
