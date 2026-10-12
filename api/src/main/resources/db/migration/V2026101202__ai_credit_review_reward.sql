-- 독후감을 쓰면 AI 질문권 1개 (책 한 권당 한 번).

-- 원장 종류에 REVIEW_REWARD 를 더한다. 자동으로 붙은 CHECK 이름을 짐작하지 않도록 찾아서 지운다.
DO $$
DECLARE c text;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'ai_credit_ledger'::regclass AND contype = 'c'
               AND pg_get_constraintdef(oid) LIKE '%FREE_GRANT%'
    LOOP
        EXECUTE 'ALTER TABLE ai_credit_ledger DROP CONSTRAINT ' || quote_ident(c);
    END LOOP;
END $$;
ALTER TABLE ai_credit_ledger ADD CONSTRAINT ai_credit_ledger_type_check
    CHECK (type IN ('FREE_GRANT', 'PURCHASE', 'USE', 'REFUND', 'ADMIN_GRANT', 'ADMIN_REVOKE', 'REVIEW_REWARD'));

-- 어느 책으로 이미 받았는지. (사용자, 책) 하나에 한 번
CREATE TABLE ai_credit_review_rewards (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    ledger_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, book_id)
);
