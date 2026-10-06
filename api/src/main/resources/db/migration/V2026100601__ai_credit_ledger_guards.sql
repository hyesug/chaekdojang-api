-- 질문권 원장의 마지막 방어선.
-- 코드가 행 잠금과 상태 검사로 중복을 막지만, 같은 결제·같은 AI 요청이 원장에 두 번 들어가는 일은
-- DB 가 직접 거절하게 한다. 운영 반영 전에 docs/ops/ai-credit-integrity.sql 의 1~3번이 0건인지 확인한다.
CREATE UNIQUE INDEX uq_ai_credit_ledger_use_ref ON ai_credit_ledger(reference_id) WHERE type = 'USE';
CREATE UNIQUE INDEX uq_ai_credit_ledger_refund_ref ON ai_credit_ledger(reference_id) WHERE type = 'REFUND';
CREATE UNIQUE INDEX uq_ai_credit_ledger_purchase_ref ON ai_credit_ledger(reference_id) WHERE type = 'PURCHASE';
