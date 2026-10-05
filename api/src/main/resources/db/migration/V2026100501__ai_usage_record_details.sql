-- 운세 AI 호출별 원가 분석용 열.
-- 기존 행은 값이 없으므로 모두 NULL 을 허용한다. 크레딧 단가 조정은 이 데이터를 모은 뒤에 한다.
ALTER TABLE ai_usage_records
    ADD COLUMN provider VARCHAR(20),
    ADD COLUMN session_id VARCHAR(64),
    ADD COLUMN question_category VARCHAR(80),
    ADD COLUMN detail_level VARCHAR(10),
    ADD COLUMN credit_used INTEGER,
    ADD COLUMN cache_write_1h_tokens INTEGER,
    ADD COLUMN context_chars INTEGER,
    ADD COLUMN focus_chars INTEGER,
    ADD COLUMN history_message_count INTEGER,
    ADD COLUMN summarized_turns INTEGER;

CREATE INDEX idx_ai_usage_records_session ON ai_usage_records(session_id) WHERE session_id IS NOT NULL;
