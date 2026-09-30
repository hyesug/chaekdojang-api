CREATE TABLE ai_credit_ledger (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL CHECK (amount <> 0),
    type VARCHAR(30) NOT NULL CHECK (type IN ('FREE_GRANT', 'PURCHASE', 'USE', 'REFUND', 'ADMIN_GRANT', 'ADMIN_REVOKE')),
    reference_id UUID,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_credit_ledger_user_created ON ai_credit_ledger(user_id, created_at DESC);
CREATE UNIQUE INDEX uq_ai_credit_free_grant ON ai_credit_ledger(user_id, type) WHERE type = 'FREE_GRANT';

CREATE TABLE ai_credit_reservations (
    request_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'COMPLETED', 'REFUNDED')),
    use_ledger_id UUID NOT NULL REFERENCES ai_credit_ledger(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX idx_ai_credit_reservations_user_created ON ai_credit_reservations(user_id, created_at DESC);

CREATE TABLE ai_usage_records (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE REFERENCES ai_credit_reservations(request_id),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature VARCHAR(80) NOT NULL,
    trigger_name VARCHAR(120) NOT NULL,
    model VARCHAR(120) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    cache_read_tokens INTEGER,
    cache_write_tokens INTEGER,
    estimated_cost NUMERIC(18, 8),
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_type VARCHAR(120)
);
CREATE INDEX idx_ai_usage_records_completed ON ai_usage_records(completed_at DESC);
CREATE INDEX idx_ai_usage_records_user_completed ON ai_usage_records(user_id, completed_at DESC);

CREATE TABLE ai_credit_policy (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    rollout_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO ai_credit_policy (id) VALUES (1);
