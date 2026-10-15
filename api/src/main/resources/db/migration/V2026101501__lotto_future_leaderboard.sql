-- 로또 미래검증 변동 순위표: 회차마다 후보 규칙 전부의 번호와 그 시점 순위·상위 5개를 저장한다.
-- 기존 lotto_future_predictions(8개 모델)는 바꾸지 않는다. 재생성은 revision 을 올리고 이전 활성 행을 끈다.
CREATE TABLE lotto_future_leaderboard_entries (
    id BIGSERIAL PRIMARY KEY,
    prediction_round_id BIGINT NOT NULL REFERENCES lotto_future_prediction_rounds(id) ON DELETE CASCADE,
    revision INTEGER NOT NULL,
    rule_id VARCHAR(40) NOT NULL,
    numbers JSONB NOT NULL,
    rank INTEGER,
    score DOUBLE PRECISION,
    record_rounds INTEGER NOT NULL,
    record_mean DOUBLE PRECISION,
    recent_mean DOUBLE PRECISION,
    top5_rank INTEGER CHECK (top5_rank BETWEEN 1 AND 5),
    model_version VARCHAR(80) NOT NULL,
    engine_version VARCHAR(120) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_lotto_future_leaderboard_revision UNIQUE (prediction_round_id, rule_id, revision)
);

CREATE UNIQUE INDEX uq_lotto_future_leaderboard_active
    ON lotto_future_leaderboard_entries (prediction_round_id, rule_id)
    WHERE active;
