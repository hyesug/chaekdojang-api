-- 예측을 계산한 운세 엔진 버전을 남긴다. 기존 행은 기록 이전 값으로 NULL 그대로 둔다.
ALTER TABLE lotto_future_predictions ADD COLUMN engine_version VARCHAR(80);
