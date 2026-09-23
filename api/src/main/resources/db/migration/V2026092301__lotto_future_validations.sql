CREATE TABLE lotto_future_prediction_rounds (
    id BIGSERIAL PRIMARY KEY,
    draw_round INTEGER NOT NULL UNIQUE,
    draw_date DATE NOT NULL,
    draw_time TIME NOT NULL,
    timezone VARCHAR(40) NOT NULL,
    time_source VARCHAR(40) NOT NULL,
    time_source_detail VARCHAR(500),
    generated_at TIMESTAMP NOT NULL,
    actual_numbers JSONB,
    result_confirmed_at TIMESTAMP
);

CREATE TABLE lotto_future_predictions (
    id BIGSERIAL PRIMARY KEY,
    prediction_round_id BIGINT NOT NULL REFERENCES lotto_future_prediction_rounds(id) ON DELETE CASCADE,
    model VARCHAR(2) NOT NULL CHECK (model IN ('A', 'D', 'E', 'F', 'H')),
    revision INTEGER NOT NULL,
    numbers JSONB NOT NULL,
    model_version VARCHAR(120) NOT NULL,
    source_commit VARCHAR(80) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_lotto_future_prediction_revision UNIQUE (prediction_round_id, model, revision)
);

CREATE UNIQUE INDEX uq_lotto_future_prediction_active
    ON lotto_future_predictions (prediction_round_id, model)
    WHERE active;

CREATE INDEX idx_lotto_future_prediction_round_date
    ON lotto_future_prediction_rounds (draw_date DESC);
