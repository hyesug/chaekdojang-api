ALTER TABLE books
    ALTER COLUMN isbn13 DROP NOT NULL,
    ADD COLUMN external_id VARCHAR(255),
    ADD COLUMN source_url VARCHAR(1000);

ALTER TABLE books DROP CONSTRAINT IF EXISTS books_source_check;
ALTER TABLE books
    ADD CONSTRAINT books_source_check
        CHECK (source IN ('KAKAO', 'GOOGLE_BOOKS', 'NAVER_SERIES', 'KAKAO_PAGE', 'RIDI', 'MUNPIA'));

CREATE UNIQUE INDEX uq_books_source_external_id
    ON books (source, external_id)
    WHERE external_id IS NOT NULL;

ALTER TABLE purchase_links DROP CONSTRAINT IF EXISTS purchase_links_provider_check;
ALTER TABLE purchase_links
    ADD CONSTRAINT purchase_links_provider_check
        CHECK (provider IN ('COUPANG', 'KYOBO', 'NAVER_SERIES', 'KAKAO_PAGE', 'RIDI', 'MUNPIA'));
