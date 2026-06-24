CREATE TABLE IF NOT EXISTS reading_groups (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(500),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    join_policy VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reading_group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES reading_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reading_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS reading_group_books (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES reading_groups(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id),
    note VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reading_group_books_group_book UNIQUE (group_id, book_id)
);

CREATE TABLE IF NOT EXISTS reading_group_reviews (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES reading_groups(id) ON DELETE CASCADE,
    group_book_id BIGINT NOT NULL REFERENCES reading_group_books(id) ON DELETE CASCADE,
    review_id BIGINT NOT NULL REFERENCES reviews(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reading_group_reviews_book_review UNIQUE (group_book_id, review_id)
);

CREATE INDEX IF NOT EXISTS idx_reading_groups_slug ON reading_groups(slug);
CREATE INDEX IF NOT EXISTS idx_reading_groups_visibility_created ON reading_groups(visibility, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reading_group_members_group_status ON reading_group_members(group_id, status);
CREATE INDEX IF NOT EXISTS idx_reading_group_members_user ON reading_group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_group_books_group ON reading_group_books(group_id);
CREATE INDEX IF NOT EXISTS idx_reading_group_reviews_group_book ON reading_group_reviews(group_book_id, created_at DESC);
