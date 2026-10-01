CREATE TABLE ai_credit_lots (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_ledger_id UUID NOT NULL UNIQUE REFERENCES ai_credit_ledger(id),
    source_type VARCHAR(30) NOT NULL,
    original_amount INTEGER NOT NULL CHECK (original_amount > 0),
    remaining_amount INTEGER NOT NULL CHECK (remaining_amount >= 0),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_credit_lots_available ON ai_credit_lots(user_id, expires_at, created_at) WHERE remaining_amount > 0;

CREATE TABLE ai_credit_reservation_allocations (
    request_id UUID NOT NULL REFERENCES ai_credit_reservations(request_id) ON DELETE CASCADE,
    lot_id UUID NOT NULL REFERENCES ai_credit_lots(id),
    amount INTEGER NOT NULL CHECK (amount > 0),
    PRIMARY KEY (request_id, lot_id)
);

CREATE TABLE ai_credit_orders (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product VARCHAR(30) NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    credits INTEGER NOT NULL CHECK (credits > 0),
    payment_id VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED')),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_credit_orders_user_created ON ai_credit_orders(user_id, created_at DESC);

-- 기존 무료 지급을 질문권 묶음으로 옮긴다. 이미 사용한 무료분은 기존 이력으로 보존되고,
-- 잔여 무료 1회만 새 묶음에 넣는다.
INSERT INTO ai_credit_lots(id, user_id, source_ledger_id, source_type, original_amount, remaining_amount)
SELECT gen_random_uuid(), l.user_id, l.id, 'FREE_GRANT', 1,
       CASE WHEN EXISTS (SELECT 1 FROM ai_credit_ledger u WHERE u.user_id=l.user_id AND u.type='USE') THEN 0 ELSE 1 END
FROM ai_credit_ledger l
WHERE l.type='FREE_GRANT';

INSERT INTO ai_credit_lots(id, user_id, source_ledger_id, source_type, original_amount, remaining_amount)
SELECT gen_random_uuid(), l.user_id, l.id, l.type, l.amount, GREATEST(0, l.amount)
FROM ai_credit_ledger l
WHERE l.type IN ('PURCHASE', 'ADMIN_GRANT');
