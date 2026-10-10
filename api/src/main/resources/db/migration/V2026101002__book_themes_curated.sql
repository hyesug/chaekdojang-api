-- 미리 골라 둔 스테디셀러(book-themes/steady-sellers.json)로 붙인 주제인가 — 추천에서 AI 가 붙인 책보다 앞에 둔다
ALTER TABLE books ADD COLUMN themes_curated BOOLEAN NOT NULL DEFAULT FALSE;
