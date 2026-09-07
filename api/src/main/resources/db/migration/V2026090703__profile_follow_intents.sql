-- 관심 독자 명단과 우선 초대
-- 서평단에 신청했다 선정되지 않은 독자는 그 책에 관심을 표명한 검증된 잠재 독자다.
-- 출판사에는 집계만 보여주고, 연락은 플랫폼이 대행한다(이메일 제3자 제공 금지).

CREATE TABLE profile_follow_intents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profile_id BIGINT NOT NULL REFERENCES official_profiles(id) ON DELETE CASCADE,
    source_campaign_id BIGINT REFERENCES review_campaigns(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    CONSTRAINT uk_profile_follow_intent UNIQUE (user_id, profile_id)
);

CREATE INDEX idx_profile_follow_intents_profile_active
    ON profile_follow_intents(profile_id)
    WHERE unsubscribed_at IS NULL;

-- 발송 이력. 캠페인당 1회, 30일 최대 2회 제한을 이 표로 판단한다.
CREATE TABLE profile_invite_sends (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES official_profiles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    campaign_id BIGINT NOT NULL REFERENCES review_campaigns(id) ON DELETE CASCADE,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_profile_invite_send_campaign UNIQUE (campaign_id, user_id)
);

CREATE INDEX idx_profile_invite_sends_profile_user_sent
    ON profile_invite_sends(profile_id, user_id, sent_at DESC);

-- 우선 초대 기간. 0이면 우선 초대 없이 바로 공개 모집한다.
ALTER TABLE review_campaigns
    ADD COLUMN priority_invite_hours INTEGER NOT NULL DEFAULT 24;

ALTER TABLE review_campaigns
    ADD COLUMN priority_invite_until TIMESTAMP;

ALTER TABLE review_campaigns
    ADD CONSTRAINT review_campaign_priority_invite_hours_check
    CHECK (priority_invite_hours >= 0 AND priority_invite_hours <= 48);

-- 우선 초대 알림 종류 추가
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'LIKE', 'COMMENT', 'FOLLOW', 'SAME_BOOK_REVIEW',
        'GROUP_JOIN_REQUEST', 'GROUP_JOINED', 'GROUP_JOIN_APPROVED',
        'REVIEW_CONTINUED',
        'CAMPAIGN_SELECTED', 'CAMPAIGN_REJECTED', 'CAMPAIGN_INVITED'
    ));
