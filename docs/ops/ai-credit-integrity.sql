-- AI 질문권 원장 무결성 점검 (읽기 전용)
-- 운영 DB 에서 그대로 실행해도 아무것도 바꾸지 않는다. 모든 쿼리는 "문제 행 수"를 돌려주며 정상이면 0 이다.
-- 사용 시점: 운영 배포 전(1~3 은 V2026100601 반영 전 필수), 배포 직후, 정산·문의 대응 때.

-- 1. 같은 AI 요청에 USE 가 두 번 이상
SELECT '1_dup_use' AS check, count(*) FROM (
  SELECT reference_id FROM ai_credit_ledger WHERE type = 'USE' GROUP BY reference_id HAVING count(*) > 1) x;

-- 2. 같은 AI 요청에 REFUND 가 두 번 이상
SELECT '2_dup_refund' AS check, count(*) FROM (
  SELECT reference_id FROM ai_credit_ledger WHERE type = 'REFUND' GROUP BY reference_id HAVING count(*) > 1) x;

-- 3. 같은 주문에 PURCHASE 가 두 번 이상
SELECT '3_dup_purchase' AS check, count(*) FROM (
  SELECT reference_id FROM ai_credit_ledger WHERE type = 'PURCHASE' AND reference_id IS NOT NULL GROUP BY reference_id HAVING count(*) > 1) x;

-- 4. 음수 잔액 또는 원래보다 큰 잔액을 가진 질문권 묶음
SELECT '4_bad_lot' AS check, count(*) FROM ai_credit_lots WHERE remaining_amount < 0 OR remaining_amount > original_amount;

-- 5. 예약마다 USE 원장이 정확히 1건이 아님
SELECT '5_reservation_use_mismatch' AS check, count(*) FROM ai_credit_reservations r
WHERE (SELECT count(*) FROM ai_credit_ledger l WHERE l.reference_id = r.request_id AND l.type = 'USE') <> 1;

-- 6. REFUND 원장이 있는데 예약 상태가 REFUNDED 가 아님 (또는 그 반대)
SELECT '6_refund_status_mismatch' AS check, count(*) FROM ai_credit_reservations r
WHERE (r.status = 'REFUNDED') <> EXISTS (SELECT 1 FROM ai_credit_ledger l WHERE l.reference_id = r.request_id AND l.type = 'REFUND');

-- 7. PAID 주문인데 PURCHASE 원장이 없음 / PURCHASE 원장이 있는데 주문이 PAID 가 아님
SELECT '7_paid_without_purchase' AS check, count(*) FROM ai_credit_orders o
WHERE o.status = 'PAID' AND NOT EXISTS (SELECT 1 FROM ai_credit_ledger l WHERE l.reference_id = o.id AND l.type = 'PURCHASE');
SELECT '7_purchase_without_paid' AS check, count(*) FROM ai_credit_ledger l
WHERE l.type = 'PURCHASE' AND l.reference_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ai_credit_orders o WHERE o.id = l.reference_id AND o.status = 'PAID');

-- 8. 원장 합계와 묶음 잔액이 어긋나는 사용자 (만료된 묶음이 있는 사용자는 만료분만큼 차이가 정상이라 뺀다)
SELECT '8_ledger_lot_mismatch' AS check, count(*) FROM (
  SELECT u.id FROM users u
  WHERE EXISTS (SELECT 1 FROM ai_credit_ledger WHERE user_id = u.id)
    AND NOT EXISTS (SELECT 1 FROM ai_credit_lots WHERE user_id = u.id AND expires_at <= CURRENT_TIMESTAMP)
    AND (SELECT COALESCE(sum(amount), 0) FROM ai_credit_ledger WHERE user_id = u.id)
     <> (SELECT COALESCE(sum(remaining_amount), 0) FROM ai_credit_lots WHERE user_id = u.id)) x;

-- 9. 관리자 조정인데 사유가 비었거나 감사 로그가 없음 (V2026100601 이전 조정은 감사 로그가 없을 수 있다)
SELECT '9_admin_adjust_without_reason' AS check, count(*) FROM ai_credit_ledger
WHERE type IN ('ADMIN_GRANT', 'ADMIN_REVOKE') AND (description IS NULL OR length(trim(description)) < 8);

-- 10. 15분 넘게 정산되지 않은 예약 — 프론트의 complete/refund 호출이 실패한 흔적이다.
--     질문권은 차감된 채로 남아 있다. AI 가 실제로 답했는지 ai_usage_records·Vercel 로그로 확인한 뒤
--     관리자 조정(+)으로 복구하거나 그대로 둔다.
SELECT '10_stale_reservations' AS check, count(*) FROM ai_credit_reservations
WHERE status = 'RESERVED' AND created_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes';

-- 11. 오래된 PENDING 주문 — 결제창에서 돈은 빠졌는데 확정 호출이 오지 않았을 수 있다.
--     PortOne 콘솔에서 payment_id 로 결제 상태를 확인한다(웹훅·자동 대사가 아직 없다).
SELECT '11_stale_pending_orders' AS check, count(*) FROM ai_credit_orders
WHERE status = 'PENDING' AND created_at < CURRENT_TIMESTAMP - INTERVAL '30 minutes';
