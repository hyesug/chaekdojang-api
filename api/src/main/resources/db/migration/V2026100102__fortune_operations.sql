CREATE TABLE ai_credit_refunds (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES ai_credit_orders(id),
    amount INTEGER NOT NULL CHECK (amount > 0),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED')),
    requested_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX idx_ai_credit_refunds_status_created ON ai_credit_refunds(status, created_at DESC);
CREATE INDEX idx_ai_credit_orders_status_created ON ai_credit_orders(status, created_at DESC);
CREATE INDEX idx_ai_credit_ledger_type_created ON ai_credit_ledger(type, created_at DESC);
CREATE INDEX idx_ai_usage_records_feature_completed ON ai_usage_records(feature, completed_at DESC);
CREATE INDEX idx_metric_events_event_created ON metric_events(event_type, created_at DESC);
CREATE INDEX idx_metric_events_user_event_created ON metric_events(user_id, event_type, created_at DESC);
