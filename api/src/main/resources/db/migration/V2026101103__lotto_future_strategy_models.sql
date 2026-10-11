-- 15체계 득표 전략 S1·S2·S3(화면명 전략 A·B·C)을 허용한다. 기존 행은 수정하지 않고 허용 목록만 넓힌다.
ALTER TABLE lotto_future_predictions DROP CONSTRAINT lotto_future_predictions_model_check;
ALTER TABLE lotto_future_predictions ADD CONSTRAINT lotto_future_predictions_model_check
    CHECK (model IN ('A', 'D', 'E', 'F', 'H', 'S1', 'S2', 'S3'));
