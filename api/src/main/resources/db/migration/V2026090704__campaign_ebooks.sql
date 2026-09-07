-- PDF·전자책 서평단
-- 인쇄비·택배비 없이 캠페인을 열 수 있게 한다.
-- 완벽한 DRM은 불가능하므로 목표는 차단이 아니라 억제와 추적이다.

ALTER TABLE review_campaigns
    ADD COLUMN delivery_type VARCHAR(20) NOT NULL DEFAULT 'PHYSICAL';

ALTER TABLE review_campaigns
    ADD CONSTRAINT review_campaign_delivery_type_check
    CHECK (delivery_type IN ('PHYSICAL', 'PDF', 'EPUB'));

-- 열람 만료 = 독후감 마감일 + 이 일수
ALTER TABLE review_campaigns
    ADD COLUMN ebook_access_extra_days INTEGER NOT NULL DEFAULT 7;

ALTER TABLE review_campaigns
    ADD CONSTRAINT review_campaign_ebook_access_extra_days_check
    CHECK (ebook_access_extra_days >= 0 AND ebook_access_extra_days <= 90);

CREATE TABLE campaign_ebook_files (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE REFERENCES review_campaigns(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    byte_size BIGINT NOT NULL,
    page_count INTEGER,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ebook_access_grants (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE REFERENCES review_campaign_applications(id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    first_opened_at TIMESTAMP,
    last_opened_at TIMESTAMP,
    open_count INTEGER NOT NULL DEFAULT 0,
    last_open_ip VARCHAR(64),
    watermark_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    watermarked_storage_key VARCHAR(500),
    revoked_at TIMESTAMP,
    CONSTRAINT ebook_grant_watermark_status_check
        CHECK (watermark_status IN ('PENDING', 'READY', 'FAILED'))
);

CREATE INDEX idx_ebook_access_grants_expires ON ebook_access_grants(expires_at);
