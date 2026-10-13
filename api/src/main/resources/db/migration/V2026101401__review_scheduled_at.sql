-- 독후감 예약 발송: 예약 시각이 오기 전에는 비공개(hidden)로 두고, 시각이 되면 스케줄러가 공개한다.
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMP NULL;
CREATE INDEX IF NOT EXISTS idx_reviews_scheduled_at ON reviews (scheduled_at) WHERE scheduled_at IS NOT NULL;
