UPDATE books
SET slug = 'book-' || id || '-' || slug
WHERE slug ~ '^[0-9]+$';
