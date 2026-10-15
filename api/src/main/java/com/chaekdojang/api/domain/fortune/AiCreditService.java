package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AiCreditService {
    private static final String FORTUNE_DEEP_QUESTION = "FORTUNE_DEEP_QUESTION";
    private final JdbcTemplate jdbc;
    private final AiCreditProperties properties;
    private final AiCostCalculator costCalculator;
    private final UserRepository userRepository;
    private final AdminAuditLogService auditLogService;

    @Transactional
    public Reservation reserve(Long userId, UUID requestId, FortuneAiModelTier tier) {
        if (tier == null) throw new CustomException(ErrorCode.INVALID_REQUEST);
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Reservation existing = reservation(userId, requestId);
        if (existing != null) return existing;
        grantFreeIfEligible(userId);
        int balance = balance(userId);
        if (balance < tier.creditCost() && isAdmin(userId)) {
            topUpAdmin(userId, tier.creditCost() - balance);
            balance = balance(userId);
        }
        if (balance < tier.creditCost()) throw new CustomException(ErrorCode.AI_CREDIT_EXHAUSTED);
        UUID ledgerId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)", ledgerId,userId,-tier.creditCost(),"USE",requestId,tier.displayName()+" 선점");
        jdbc.update("INSERT INTO ai_credit_reservations(request_id,user_id,feature,status,use_ledger_id) VALUES (?,?,?,?,?)", requestId,userId,FORTUNE_DEEP_QUESTION+":"+tier.name(),"RESERVED",ledgerId);
        allocateReservation(userId, requestId, tier.creditCost());
        return new Reservation(requestId, balance(userId), freeRemaining(userId), "RESERVED", LocalDateTime.now(), tier, tier.creditCost());
    }

    @Transactional
    public void complete(Long userId, UUID requestId, Usage usage) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Reservation r = requiredReservation(userId, requestId);
        if ("COMPLETED".equals(r.status())) return;
        if ("REFUNDED".equals(r.status())) throw new CustomException(ErrorCode.INVALID_REQUEST);
        LocalDateTime now = LocalDateTime.now();
        FortuneAiModelTier tier = r.tier();
        CallDetail d = usage.detail() == null ? CallDetail.EMPTY : usage.detail();
        int write1h = d.cacheWrite1hTokens() == null ? 0 : d.cacheWrite1hTokens();
        BigDecimal cost = costCalculator.estimate(tier, usage.inputTokens(), usage.outputTokens(), usage.cacheReadTokens(), usage.cacheWriteTokens(), write1h);
        jdbc.update("UPDATE ai_credit_reservations SET status='COMPLETED', completed_at=? WHERE request_id=?", Timestamp.valueOf(now),requestId);
        jdbc.update("INSERT INTO ai_usage_records(id,request_id,user_id,feature,trigger_name,model,input_tokens,output_tokens,cache_read_tokens,cache_write_tokens,estimated_cost,requested_at,completed_at,duration_ms,success,provider,session_id,question_category,detail_level,credit_used,cache_write_1h_tokens,context_chars,focus_chars,history_message_count,summarized_turns) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,true,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(),requestId,userId,FORTUNE_DEEP_QUESTION,tier.displayName(),tier.model(),usage.inputTokens(),usage.outputTokens(),usage.cacheReadTokens(),usage.cacheWriteTokens(),cost,Timestamp.valueOf(usage.requestedAt()),Timestamp.valueOf(now),Math.max(0,usage.durationMs()),tier.provider(),clip(d.sessionId(),64),clip(d.questionCategory(),80),clip(d.detailLevel(),10),r.creditsReserved(),d.cacheWrite1hTokens(),d.contextChars(),d.focusChars(),d.historyMessageCount(),d.summarizedTurns());
    }

    /** 답을 받다 연결이 끊긴 질문 — 질문권 1회는 질문과 그 답변 한 벌이라 돌려준다. 악용을 막아 하루 이만큼만 */
    static final String MID_ANSWER_ABORT = "CLIENT_ABORTED_MID_ANSWER";
    static final int MID_ANSWER_REFUNDS_PER_DAY = 2;

    /**
     * 실패한 호출의 질문권을 돌려준다.
     * 답을 받다 끊긴 경우(MID_ANSWER_ABORT)는 하루 {@link #MID_ANSWER_REFUNDS_PER_DAY}번까지만 돌려주고,
     * 넘으면 돌려주지 않는다 — 그때는 호출한 쪽이 사용으로 확정한다.
     * @return 돌려줬으면 true
     */
    @Transactional
    public boolean refund(Long userId, UUID requestId, String errorType) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Reservation r = requiredReservation(userId, requestId);
        if (!"RESERVED".equals(r.status())) return "REFUNDED".equals(r.status());
        if (errorType != null && errorType.startsWith(MID_ANSWER_ABORT)) {
            Integer today = jdbc.queryForObject("SELECT COUNT(*) FROM ai_credit_ledger WHERE user_id=? AND type='REFUND' AND description LIKE ? AND created_at > CURRENT_TIMESTAMP - INTERVAL '1 day'",
                    Integer.class, userId, "%" + MID_ANSWER_ABORT + "%");
            if (today != null && today >= MID_ANSWER_REFUNDS_PER_DAY) return false;
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE ai_credit_reservations SET status='REFUNDED', completed_at=? WHERE request_id=?",Timestamp.valueOf(now),requestId);
        jdbc.update("UPDATE ai_credit_lots l SET remaining_amount = l.remaining_amount + a.amount FROM ai_credit_reservation_allocations a WHERE a.request_id=? AND a.lot_id=l.id", requestId);
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),userId,r.creditsReserved(),"REFUND",requestId,"AI 호출 실패 자동 복구: "+sanitize(errorType));
        // success 는 false 고정, 마지막 자리가 error_type 이다. 순서가 어긋나면 복구 트랜잭션 전체가 롤백된다
        jdbc.update("INSERT INTO ai_usage_records(id,request_id,user_id,feature,trigger_name,model,requested_at,completed_at,duration_ms,success,error_type) VALUES (?,?,?,?,?,?,?,?,?,false,?)",UUID.randomUUID(),requestId,userId,FORTUNE_DEEP_QUESTION,r.tier().displayName(),r.tier().model(),Timestamp.valueOf(r.createdAt()),Timestamp.valueOf(now),Math.max(0,now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()-r.createdAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),sanitize(errorType));
        return true;
    }

    @Transactional
    public void adjust(Long adminId, Long userId, int amount, String description) {
        if (amount == 0 || description == null || description.isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST);
        if (jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE",Long.class,userId).isEmpty()) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        if (amount < 0 && balance(userId) + amount < 0) throw new CustomException(ErrorCode.AI_CREDIT_EXHAUSTED);
        UUID ledgerId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)",ledgerId,userId,amount,amount > 0 ? "ADMIN_GRANT":"ADMIN_REVOKE",null,"관리자 "+adminId+": "+sanitize(description));
        if (amount > 0) createLot(userId, ledgerId, "ADMIN_GRANT", amount, null);
        else consumeLots(userId, -amount);
        // 질문권은 돈과 같은 값이라 누가 언제 왜 바꿨는지 관리자 감사 로그에도 남긴다
        User admin = userRepository.findById(adminId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        auditLogService.record(admin, amount > 0 ? "AI_CREDIT_GRANTED" : "AI_CREDIT_REVOKED", "USER", userId,
                (amount > 0 ? "+" : "") + amount + "회 (원장 " + ledgerId + "): " + sanitize(description));
    }

    /** 관리자 문의 대응용. 잔액·최근 원장·주문·정산이 안 끝난 예약을 한 번에 본다 */
    @Transactional(readOnly = true)
    public AdminView adminView(Long userId) {
        if (jdbc.queryForList("SELECT id FROM users WHERE id = ?",Long.class,userId).isEmpty()) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        List<AdminLedger> ledger = jdbc.query("SELECT id,amount,type,reference_id,description,created_at FROM ai_credit_ledger WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
                (rs,n)->new AdminLedger(UUID.fromString(rs.getString(1)),rs.getInt(2),rs.getString(3),rs.getString(4)==null?null:UUID.fromString(rs.getString(4)),rs.getString(5),rs.getTimestamp(6).toLocalDateTime()),userId);
        List<AdminOrder> orders = jdbc.query("SELECT id,product,amount,credits,payment_id,status,paid_at,created_at FROM ai_credit_orders WHERE user_id=? ORDER BY created_at DESC LIMIT 50",
                (rs,n)->new AdminOrder(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7)==null?null:rs.getTimestamp(7).toLocalDateTime(),rs.getTimestamp(8).toLocalDateTime()),userId);
        Integer stale = jdbc.queryForObject("SELECT count(*) FROM ai_credit_reservations WHERE user_id=? AND status='RESERVED' AND created_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes'",Integer.class,userId);
        return new AdminView(new Balance(freeRemaining(userId), purchasedBalance(userId), balance(userId), isAdmin(userId)), ledger, orders, stale == null ? 0 : stale);
    }
    /**
     * 질문권 기록이 있는 회원 목록 — 잔액·사용 횟수·마지막 사용을 한 줄씩. 최근에 쓴 회원부터.
     * q 는 닉네임·이메일 일부나 회원 ID.
     */
    @Transactional(readOnly = true)
    public UserPage userList(String q, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String keyword = q == null ? "" : q.trim();
        String where = "u.deleted_at IS NULL AND EXISTS (SELECT 1 FROM ai_credit_ledger x WHERE x.user_id = u.id)"
                + (keyword.isEmpty() ? "" : " AND (u.nickname ILIKE ? OR u.email ILIKE ? OR CAST(u.id AS TEXT) = ?)");
        Object[] filter = keyword.isEmpty() ? new Object[0] : new Object[]{"%" + keyword + "%", "%" + keyword + "%", keyword};
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM users u WHERE " + where, Long.class, filter);
        List<Object> args = new ArrayList<>(Arrays.asList(filter));
        args.add(safeSize);
        args.add((long) safePage * safeSize);
        List<UserRow> rows = jdbc.query("""
                SELECT u.id, u.nickname, u.email, u.role,
                       COALESCE((SELECT SUM(l.remaining_amount) FROM ai_credit_lots l WHERE l.user_id=u.id AND (l.expires_at IS NULL OR l.expires_at > CURRENT_TIMESTAMP)),0),
                       COALESCE((SELECT SUM(l.remaining_amount) FROM ai_credit_lots l WHERE l.user_id=u.id AND l.source_type IN ('FREE_GRANT','REVIEW_REWARD') AND (l.expires_at IS NULL OR l.expires_at > CURRENT_TIMESTAMP)),0),
                       a.uses, a.failures, a.last_used_at, a.cost
                FROM users u
                LEFT JOIN LATERAL (
                    SELECT COUNT(*) FILTER (WHERE r.success) AS uses, COUNT(*) FILTER (WHERE NOT r.success) AS failures,
                           MAX(r.completed_at) AS last_used_at, COALESCE(SUM(r.estimated_cost),0) AS cost
                    FROM ai_usage_records r WHERE r.user_id = u.id
                ) a ON TRUE
                WHERE""" + " " + where + " " + """
                 ORDER BY a.last_used_at DESC NULLS LAST, u.id DESC LIMIT ? OFFSET ?""",
                (rs, n) -> {
                    int totalBalance = rs.getInt(5), free = rs.getInt(6);
                    Timestamp last = rs.getTimestamp(9);
                    return new UserRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4) != null && rs.getString(4).endsWith("ADMIN"),
                            totalBalance, free, Math.max(0, totalBalance - free), rs.getLong(7), rs.getLong(8),
                            last == null ? null : last.toLocalDateTime(), rs.getBigDecimal(10));
                }, args.toArray());
        return new UserPage(rows, total == null ? 0 : total, safePage, safeSize);
    }

    /** 회원 한 명의 AI 질문 이용 기록 — 질문·답 본문은 저장하지 않으므로 언제·어떤 풀이·분류·성공 여부만 */
    @Transactional(readOnly = true)
    public List<UsageRow> usageHistory(Long userId, int limit) {
        if (jdbc.queryForList("SELECT id FROM users WHERE id = ?",Long.class,userId).isEmpty()) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        return jdbc.query("SELECT request_id,trigger_name,model,question_category,detail_level,session_id,credit_used,success,error_type,input_tokens,output_tokens,estimated_cost,duration_ms,requested_at FROM ai_usage_records WHERE user_id=? ORDER BY requested_at DESC LIMIT ?",
                (rs,n)->new UsageRow(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),(Integer)rs.getObject(7),rs.getBoolean(8),rs.getString(9),(Integer)rs.getObject(10),(Integer)rs.getObject(11),rs.getBigDecimal(12),rs.getLong(13),rs.getTimestamp(14).toLocalDateTime()),
                userId, Math.min(Math.max(limit, 1), 500));
    }

    @Transactional
    public Balance balanceView(Long userId) { jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId); grantFreeIfEligible(userId); return new Balance(freeRemaining(userId), purchasedBalance(userId), balance(userId), isAdmin(userId)); }

    /** 관리자는 질문권 제한 없이 묻는다 — 모자라면 그만큼 자동으로 채운다(원장에 남겨 원가 집계는 그대로) */
    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }

    private void topUpAdmin(Long userId, int amount) {
        if (amount <= 0) return;
        UUID ledgerId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)", ledgerId, userId, amount, "ADMIN_GRANT", null, "관리자 무제한 — 자동 충전");
        createLot(userId, ledgerId, "ADMIN_GRANT", amount, null);
    }
    public List<History> history(Long userId) { return jdbc.query("SELECT amount,type,description,created_at FROM ai_credit_ledger WHERE user_id=? ORDER BY created_at DESC LIMIT 30",(rs,n)->new History(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toLocalDateTime()),userId); }
    public Statistics statistics() { return jdbc.queryForObject("SELECT COUNT(*), COUNT(*) FILTER (WHERE success), COUNT(*) FILTER (WHERE NOT success), COALESCE(AVG(input_tokens),0), COALESCE(AVG(output_tokens),0), COALESCE(AVG(estimated_cost),0), COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY estimated_cost),0), COALESCE(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY estimated_cost),0), COALESCE(SUM(estimated_cost),0) FROM ai_usage_records", (rs,n)->new Statistics(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getDouble(4),rs.getDouble(5),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getBigDecimal(9))); }
    private void grantFreeIfEligible(Long userId) { Long count=jdbc.queryForObject("SELECT COUNT(*) FROM ai_credit_ledger WHERE user_id=? AND type='FREE_GRANT'",Long.class,userId); if(count!=null&&count>0)return; Boolean existing=jdbc.queryForObject("SELECT u.created_at < p.rollout_at FROM users u CROSS JOIN ai_credit_policy p WHERE u.id=?",Boolean.class,userId); if(Boolean.TRUE.equals(existing)&&!properties.isGrantExistingUsers())return; UUID ledgerId=UUID.randomUUID(); jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,description) VALUES (?,?,?,?,?)",ledgerId,userId,1,"FREE_GRANT","AI 심층질문 최초 무료 1회"); createLot(userId,ledgerId,"FREE_GRANT",1,null); }
    private int balance(Long userId){ Integer v=jdbc.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM ai_credit_lots WHERE user_id=? AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)",Integer.class,userId);return v==null?0:v; }
    /** 무료로 받은 것 — 최초 1회와 독후감 보상 */
    private int freeRemaining(Long userId){ Integer v=jdbc.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM ai_credit_lots WHERE user_id=? AND source_type IN ('FREE_GRANT','REVIEW_REWARD') AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)",Integer.class,userId);return v==null?0:v; }
    private int purchasedBalance(Long userId){ return Math.max(0,balance(userId)-freeRemaining(userId)); }

    /** 독후감 한 편으로 받는 질문권 — 공백을 줄인 본문이 이 글자 수 이상일 때 */
    static final int REVIEW_REWARD_MIN_CHARS = 100;

    /**
     * 독후감을 쓰면 질문권 1개. **책 한 권당 한 번** — 같은 책으로 다시 쓰거나 지웠다 다시 써도 더 주지 않는다.
     * 책을 고르지 않은 글과 100자 미만인 글은 주지 않는다. 짧게 썼다가 100자 넘게 고치면 그때 준다.
     * @return 이번에 지급했으면 true
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean grantReviewReward(Long userId, Long reviewId, Long bookId, String content) {
        if (userId == null || reviewId == null || bookId == null) return false;
        if (reviewTextLength(content) < REVIEW_REWARD_MIN_CHARS) return false;
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Integer done = jdbc.queryForObject("SELECT COUNT(*) FROM ai_credit_review_rewards WHERE user_id=? AND book_id=?", Integer.class, userId, bookId);
        if (done != null && done > 0) return false;
        UUID ledgerId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)", ledgerId, userId, 1, "REVIEW_REWARD", null, "독후감 작성 보상 (독후감 " + reviewId + ")");
        jdbc.update("INSERT INTO ai_credit_review_rewards(user_id,book_id,review_id,ledger_id) VALUES (?,?,?,?)", userId, bookId, reviewId, ledgerId);
        createLot(userId, ledgerId, "REVIEW_REWARD", 1, null);
        return true;
    }

    /** 태그를 걷고 공백을 한 칸으로 줄인 글자 수 */
    static int reviewTextLength(String content) {
        if (content == null) return 0;
        String text = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
        return text.codePointCount(0, text.length());
    }

    @Transactional
    public void grantPurchase(Long userId, UUID orderId, AiCreditProduct product) {
        UUID ledgerId = UUID.randomUUID();
        LocalDateTime expiresAt = product.validDays() == 0 ? null : LocalDateTime.now().plusDays(product.validDays());
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)", ledgerId,userId,product.credits(),"PURCHASE",orderId,product.displayName()+" 구매");
        createLot(userId, ledgerId, "PURCHASE", product.credits(), expiresAt);
    }

    private void createLot(Long userId, UUID ledgerId, String sourceType, int amount, LocalDateTime expiresAt) {
        jdbc.update("INSERT INTO ai_credit_lots(id,user_id,source_ledger_id,source_type,original_amount,remaining_amount,expires_at) VALUES (?,?,?,?,?,?,?)", UUID.randomUUID(), userId, ledgerId, sourceType, amount, amount, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
    }

    private void allocateReservation(Long userId, UUID requestId, int amount) { consumeLots(userId, amount, requestId); }
    private void consumeLots(Long userId, int amount) { consumeLots(userId, amount, null); }
    private void consumeLots(Long userId, int amount, UUID requestId) {
        int left = amount;
        List<Lot> lots = jdbc.query("SELECT id,remaining_amount FROM ai_credit_lots WHERE user_id=? AND remaining_amount>0 AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP) ORDER BY expires_at NULLS LAST, created_at FOR UPDATE", (rs,n)->new Lot(UUID.fromString(rs.getString(1)),rs.getInt(2)), userId);
        for (Lot lot : lots) { if (left == 0) break; int used=Math.min(left,lot.remaining()); jdbc.update("UPDATE ai_credit_lots SET remaining_amount=remaining_amount-? WHERE id=?",used,lot.id()); if(requestId!=null) jdbc.update("INSERT INTO ai_credit_reservation_allocations(request_id,lot_id,amount) VALUES (?,?,?)",requestId,lot.id(),used); left-=used; }
        if(left!=0) throw new CustomException(ErrorCode.AI_CREDIT_EXHAUSTED);
    }
    private record Lot(UUID id, int remaining) {}
    private Reservation reservation(Long userId,UUID requestId){ List<Reservation> rows=jdbc.query("SELECT request_id,status,created_at,feature FROM ai_credit_reservations WHERE user_id=? AND request_id=?",(rs,n)->{ FortuneAiModelTier tier=FortuneAiModelTier.valueOf(rs.getString(4).substring(rs.getString(4).lastIndexOf(':')+1)); return new Reservation(UUID.fromString(rs.getString(1)),balance(userId),freeRemaining(userId),rs.getString(2),rs.getTimestamp(3).toLocalDateTime(),tier,tier.creditCost()); },userId,requestId);return rows.isEmpty()?null:rows.getFirst(); }
    private Reservation requiredReservation(Long userId,UUID requestId){ Reservation r=reservation(userId,requestId);if(r==null)throw new CustomException(ErrorCode.INVALID_REQUEST);return r; }
    private String sanitize(String value){return value==null||value.isBlank()?"UNKNOWN":value.substring(0,Math.min(value.length(),120));}
    private String clip(String value,int max){return value==null||value.isBlank()?null:value.substring(0,Math.min(value.length(),max));}
    public record Reservation(UUID requestId,int totalBalance,int freeRemaining,String status,LocalDateTime createdAt,FortuneAiModelTier tier,int creditsReserved){ public Reservation(UUID id,int b,int f,String s){this(id,b,f,s,LocalDateTime.now(),FortuneAiModelTier.CLAUDE_SONNET,1);} }
    public record Usage(String model,int inputTokens,int outputTokens,int cacheReadTokens,int cacheWriteTokens,long durationMs,LocalDateTime requestedAt,CallDetail detail) {
        public Usage(String model,int inputTokens,int outputTokens,int cacheReadTokens,int cacheWriteTokens,long durationMs,LocalDateTime requestedAt){this(model,inputTokens,outputTokens,cacheReadTokens,cacheWriteTokens,durationMs,requestedAt,CallDetail.EMPTY);}
    }
    /** 원가 분석용 부가 정보. 옛 클라이언트는 보내지 않으므로 전부 비어 있을 수 있다 */
    public record CallDetail(String sessionId,String questionCategory,String detailLevel,Integer cacheWrite1hTokens,Integer contextChars,Integer focusChars,Integer historyMessageCount,Integer summarizedTurns) {
        public static final CallDetail EMPTY = new CallDetail(null,null,null,null,null,null,null,null);
    }
    /** unlimited — 관리자라 잔액과 관계없이 물을 수 있다 */
    public record Balance(int freeRemaining,int purchasedBalance,int totalBalance,boolean unlimited) {}
    public record AdminLedger(UUID id,int amount,String type,UUID referenceId,String description,LocalDateTime createdAt) {}
    public record AdminOrder(UUID orderId,String product,int amount,int credits,String paymentId,String status,LocalDateTime paidAt,LocalDateTime createdAt) {}
    public record AdminView(Balance balance,List<AdminLedger> ledger,List<AdminOrder> orders,int staleReservations) {}
    public record UserRow(long userId,String nickname,String email,boolean admin,int totalBalance,int freeRemaining,int purchasedBalance,long uses,long failures,LocalDateTime lastUsedAt,BigDecimal totalCost) {}
    public record UserPage(List<UserRow> users,long total,int page,int size) {}
    public record UsageRow(UUID requestId,String tier,String model,String questionCategory,String detailLevel,String sessionId,Integer creditUsed,boolean success,String errorType,Integer inputTokens,Integer outputTokens,BigDecimal estimatedCost,long durationMs,LocalDateTime requestedAt) {}
    public record History(int amount,String type,String description,LocalDateTime createdAt) {}
    public record Statistics(long calls,long successes,long failures,double averageInputTokens,double averageOutputTokens,BigDecimal averageCost,BigDecimal p50Cost,BigDecimal p95Cost,BigDecimal totalCost) {}
}
