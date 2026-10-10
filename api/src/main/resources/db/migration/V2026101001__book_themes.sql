-- 책의 주제 태그 — 소개글을 읽고 붙인다(BookThemeService). 운세 리포트가 그 사람에게 필요한 주제로 책을 고를 때 쓴다.
-- 한 책에 1~3개. themes_tagged_at 이 있으면 태그를 붙여 본 책이다(주제가 없다고 판단한 책도 다시 보내지 않는다).
CREATE TABLE book_themes (
    book_id BIGINT NOT NULL REFERENCES books(id),
    theme VARCHAR(30) NOT NULL,
    PRIMARY KEY (book_id, theme)
);

CREATE INDEX idx_book_themes_theme ON book_themes(theme);

ALTER TABLE books ADD COLUMN themes_tagged_at TIMESTAMP;
