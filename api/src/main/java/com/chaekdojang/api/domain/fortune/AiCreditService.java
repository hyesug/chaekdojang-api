package com.chaekdojang.api.domain.fortune;

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

    @Transactional
    public Reservation reserve(Long userId, UUID requestId, FortuneAiModelTier tier) {
        if (tier == null) throw new CustomException(ErrorCode.INVALID_REQUEST);
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Reservation existing = reservation(userId, requestId);
        if (existing != null) return existing;
        grantFreeIfEligible(userId);
        int balance = balance(userId);
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

    @Transactional
    public void refund(Long userId, UUID requestId, String errorType) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        Reservation r = requiredReservation(userId, requestId);
        if (!"RESERVED".equals(r.status())) return;
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE ai_credit_reservations SET status='REFUNDED', completed_at=? WHERE request_id=?",Timestamp.valueOf(now),requestId);
        jdbc.update("UPDATE ai_credit_lots l SET remaining_amount = l.remaining_amount + a.amount FROM ai_credit_reservation_allocations a WHERE a.request_id=? AND a.lot_id=l.id", requestId);
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),userId,r.creditsReserved(),"REFUND",requestId,"AI 호출 실패 자동 복구: "+sanitize(errorType));
        jdbc.update("INSERT INTO ai_usage_records(id,request_id,user_id,feature,trigger_name,model,requested_at,completed_at,duration_ms,success,error_type) VALUES (?,?,?,?,?,?,?,?,?,?,false)",UUID.randomUUID(),requestId,userId,FORTUNE_DEEP_QUESTION,r.tier().displayName(),r.tier().model(),Timestamp.valueOf(r.createdAt()),Timestamp.valueOf(now),Math.max(0,now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()-r.createdAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),sanitize(errorType));
    }

    @Transactional
    public void adjust(Long adminId, Long userId, int amount, String description) {
        if (amount == 0 || description == null || description.isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST);
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE",Long.class,userId);
        if (amount < 0 && balance(userId) + amount < 0) throw new CustomException(ErrorCode.AI_CREDIT_EXHAUSTED);
        UUID ledgerId = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,reference_id,description) VALUES (?,?,?,?,?,?)",ledgerId,userId,amount,amount > 0 ? "ADMIN_GRANT":"ADMIN_REVOKE",null,"관리자 "+adminId+": "+sanitize(description));
        if (amount > 0) createLot(userId, ledgerId, "ADMIN_GRANT", amount, null);
        else consumeLots(userId, -amount);
    }
    @Transactional
    public Balance balanceView(Long userId) { jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId); grantFreeIfEligible(userId); return new Balance(freeRemaining(userId), purchasedBalance(userId), balance(userId)); }
    public List<History> history(Long userId) { return jdbc.query("SELECT amount,type,description,created_at FROM ai_credit_ledger WHERE user_id=? ORDER BY created_at DESC LIMIT 30",(rs,n)->new History(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toLocalDateTime()),userId); }
    public Statistics statistics() { return jdbc.queryForObject("SELECT COUNT(*), COUNT(*) FILTER (WHERE success), COUNT(*) FILTER (WHERE NOT success), COALESCE(AVG(input_tokens),0), COALESCE(AVG(output_tokens),0), COALESCE(AVG(estimated_cost),0), COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY estimated_cost),0), COALESCE(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY estimated_cost),0), COALESCE(SUM(estimated_cost),0) FROM ai_usage_records", (rs,n)->new Statistics(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getDouble(4),rs.getDouble(5),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getBigDecimal(9))); }
    private void grantFreeIfEligible(Long userId) { Long count=jdbc.queryForObject("SELECT COUNT(*) FROM ai_credit_ledger WHERE user_id=? AND type='FREE_GRANT'",Long.class,userId); if(count!=null&&count>0)return; Boolean existing=jdbc.queryForObject("SELECT u.created_at < p.rollout_at FROM users u CROSS JOIN ai_credit_policy p WHERE u.id=?",Boolean.class,userId); if(Boolean.TRUE.equals(existing)&&!properties.isGrantExistingUsers())return; UUID ledgerId=UUID.randomUUID(); jdbc.update("INSERT INTO ai_credit_ledger(id,user_id,amount,type,description) VALUES (?,?,?,?,?)",ledgerId,userId,1,"FREE_GRANT","AI 심층질문 최초 무료 1회"); createLot(userId,ledgerId,"FREE_GRANT",1,null); }
    private int balance(Long userId){ Integer v=jdbc.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM ai_credit_lots WHERE user_id=? AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)",Integer.class,userId);return v==null?0:v; }
    private int freeRemaining(Long userId){ Integer v=jdbc.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM ai_credit_lots WHERE user_id=? AND source_type='FREE_GRANT' AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)",Integer.class,userId);return v==null?0:v; }
    private int purchasedBalance(Long userId){ return Math.max(0,balance(userId)-freeRemaining(userId)); }

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
    public record Balance(int freeRemaining,int purchasedBalance,int totalBalance) {}
    public record History(int amount,String type,String description,LocalDateTime createdAt) {}
    public record Statistics(long calls,long successes,long failures,double averageInputTokens,double averageOutputTokens,BigDecimal averageCost,BigDecimal p50Cost,BigDecimal p95Cost,BigDecimal totalCost) {}
}
