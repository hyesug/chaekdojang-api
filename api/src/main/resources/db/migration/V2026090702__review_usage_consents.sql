-- 독후감 활용 동의 이력
-- 덮어쓰지 않고 append-only로 쌓는다. 분쟁 시 "언제, 어떤 약관 버전에, 무엇에 동의했는가"를
-- 재현할 수 있어야 하기 때문이다. 동의 변경은 이전 행을 revoke하고 새 행을 추가한다.

CREATE TABLE review_usage_consents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES review_campaign_applications(id) ON DELETE CASCADE,
    review_id BIGINT REFERENCES reviews(id) ON DELETE SET NULL,
    consent_promotional BOOLEAN NOT NULL DEFAULT FALSE,
    consent_excerpt BOOLEAN NOT NULL DEFAULT FALSE,
    display_name_type VARCHAR(20) NOT NULL DEFAULT 'REAL_NICKNAME',
    terms_version VARCHAR(40) NOT NULL,
    consented_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consent_ip VARCHAR(64),
    revoked_at TIMESTAMP,
    CONSTRAINT review_usage_consent_display_name_check
        CHECK (display_name_type IN ('REAL_NICKNAME', 'ANONYMOUS'))
);

CREATE INDEX idx_review_usage_consents_application
    ON review_usage_consents(application_id, consented_at DESC);

-- 신청 1건에 유효한 동의는 최대 1개
CREATE UNIQUE INDEX uk_review_usage_consents_active
    ON review_usage_consents(application_id)
    WHERE revoked_at IS NULL;
