-- 이미 써 둔 독후감에도 AI 질문권을 소급해서 준다 — 새 독후감과 같은 규칙.
--   · 책을 고른 독후감, 지워지지 않은 것, 탈퇴하지 않은 회원
--   · 본문을 태그 걷고 공백을 한 칸으로 줄여 100자 이상 (AiCreditService.reviewTextLength 와 같은 셈)
--   · (회원, 책) 하나에 한 번 — 같은 책 독후감이 여럿이면 가장 먼저 쓴 것 하나로
-- 한 번만 돈다(Flyway). 이미 받은 (회원, 책)은 건너뛴다.

CREATE TEMP TABLE review_reward_backfill ON COMMIT DROP AS
SELECT DISTINCT ON (r.author_id, r.book_id)
       r.author_id AS user_id, r.book_id, r.id AS review_id, gen_random_uuid() AS ledger_id
FROM reviews r
JOIN users u ON u.id = r.author_id
WHERE r.deleted_at IS NULL
  AND r.book_id IS NOT NULL
  AND u.deleted_at IS NULL
  AND char_length(btrim(regexp_replace(regexp_replace(r.content, '<[^>]+>', ' ', 'g'), '\s+', ' ', 'g'))) >= 100
  AND NOT EXISTS (SELECT 1 FROM ai_credit_review_rewards w WHERE w.user_id = r.author_id AND w.book_id = r.book_id)
ORDER BY r.author_id, r.book_id, r.created_at, r.id;

INSERT INTO ai_credit_ledger (id, user_id, amount, type, reference_id, description)
SELECT ledger_id, user_id, 1, 'REVIEW_REWARD', NULL, '독후감 작성 보상 (소급, 독후감 ' || review_id || ')'
FROM review_reward_backfill;

INSERT INTO ai_credit_review_rewards (user_id, book_id, review_id, ledger_id)
SELECT user_id, book_id, review_id, ledger_id
FROM review_reward_backfill;

INSERT INTO ai_credit_lots (id, user_id, source_ledger_id, source_type, original_amount, remaining_amount)
SELECT gen_random_uuid(), user_id, ledger_id, 'REVIEW_REWARD', 1, 1
FROM review_reward_backfill;
