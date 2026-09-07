-- 열람별 IP/시각/판본을 보존한다. 과거 last_open_ip로 전체 열람 이력을 추정하지 않는다.
CREATE TABLE ebook_open_logs (
    id BIGSERIAL PRIMARY KEY,
    grant_id BIGINT NOT NULL REFERENCES ebook_access_grants(id) ON DELETE CASCADE,
    opened_at TIMESTAMP NOT NULL,
    ip VARCHAR(64),
    source_storage_key VARCHAR(500) NOT NULL
);
CREATE INDEX idx_ebook_open_logs_grant_opened ON ebook_open_logs(grant_id, opened_at);

ALTER TABLE review_campaigns ADD COLUMN priority_invite_sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE review_campaigns ADD COLUMN priority_invites_sent_at TIMESTAMP;

-- 이미 공개된 과거 캠페인은 다시 발송하지 않는다.
UPDATE review_campaigns
SET priority_invites_sent_at = COALESCE(
    (SELECT MIN(s.sent_at) FROM profile_invite_sends s WHERE s.campaign_id = review_campaigns.id),
    updated_at)
WHERE priority_invite_until IS NOT NULL AND recruit_start_at <= CURRENT_TIMESTAMP;

-- 예약된 기존 모집은 시작일 기준으로 우선 기간을 복구한다.
UPDATE review_campaigns c
SET priority_invite_until = c.recruit_start_at + c.priority_invite_hours * INTERVAL '1 hour',
    priority_invite_sender_id = COALESCE(
        (SELECT n.sender_id FROM notifications n
         WHERE n.type = 'CAMPAIGN_INVITED' AND n.target_id = c.id ORDER BY n.id LIMIT 1),
        (SELECT MIN(m.user_id) FROM official_profile_members m WHERE m.profile_id = c.profile_id))
WHERE c.status = 'RECRUITING' AND c.recruit_start_at > CURRENT_TIMESTAMP AND c.priority_invite_hours > 0;
