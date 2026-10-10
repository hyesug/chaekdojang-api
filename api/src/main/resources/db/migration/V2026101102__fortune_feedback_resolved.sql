-- 처리한 피드백(사전 문장을 고친 것)은 지우지 않고 처리 시각만 남긴다 — 관리자 화면과 집계에서는 숨긴다
ALTER TABLE fortune_feedback ADD COLUMN resolved_at TIMESTAMP;
CREATE INDEX idx_fortune_feedback_resolved ON fortune_feedback (resolved_at);
