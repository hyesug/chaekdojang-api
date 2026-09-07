-- 책도장단(서평단) 기본 구조
-- 캠페인 → 신청 → 선정 → 독후감 제출 흐름과, 완주율 지표의 원천이 되는 단계별 시각을 기록한다.

CREATE TABLE review_campaigns (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES official_profiles(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    recruit_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    recruit_start_at TIMESTAMP NOT NULL,
    recruit_end_at TIMESTAMP NOT NULL,
    review_due_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_campaign_status_check
        CHECK (status IN ('DRAFT', 'RECRUITING', 'CLOSED', 'SELECTED', 'COMPLETED')),
    CONSTRAINT review_campaign_recruit_count_check CHECK (recruit_count > 0)
);

CREATE INDEX idx_review_campaigns_status_recruit_end
    ON review_campaigns(status, recruit_end_at DESC);

CREATE INDEX idx_review_campaigns_profile_created
    ON review_campaigns(profile_id, created_at DESC);

CREATE TABLE review_campaign_applications (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES review_campaigns(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_id BIGINT REFERENCES reviews(id) ON DELETE SET NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    selected_at TIMESTAMP,
    rejected_at TIMESTAMP,
    submitted_at TIMESTAMP,
    dropped_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_campaign_application_status_check
        CHECK (status IN ('APPLIED', 'SELECTED', 'REJECTED', 'SUBMITTED', 'DROPPED')),
    CONSTRAINT uk_review_campaign_application UNIQUE (campaign_id, user_id)
);

CREATE INDEX idx_campaign_applications_campaign_status
    ON review_campaign_applications(campaign_id, status);

CREATE INDEX idx_campaign_applications_user_applied
    ON review_campaign_applications(user_id, applied_at DESC);

-- 선정·미선정 알림 종류 추가
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'LIKE', 'COMMENT', 'FOLLOW', 'SAME_BOOK_REVIEW',
        'GROUP_JOIN_REQUEST', 'GROUP_JOINED', 'GROUP_JOIN_APPROVED',
        'REVIEW_CONTINUED',
        'CAMPAIGN_SELECTED', 'CAMPAIGN_REJECTED'
    ));
