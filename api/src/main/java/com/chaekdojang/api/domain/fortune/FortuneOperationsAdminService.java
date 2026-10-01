package com.chaekdojang.api.domain.fortune;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FortuneOperationsAdminService {
    private final JdbcTemplate jdbc;

    public Summary summary(LocalDateTime from, LocalDateTime to) {
        return jdbc.queryForObject("""
            SELECT
              (SELECT count(*) FROM users WHERE created_at>=? AND created_at<?),
              (SELECT count(DISTINCT coalesce(user_id::text, session_id)) FROM metric_events WHERE event_type='CHART_GENERATED' AND created_at>=? AND created_at<?),
              (SELECT count(DISTINCT user_id) FROM ai_usage_records WHERE completed_at>=? AND completed_at<?),
              (SELECT count(DISTINCT user_id) FROM ai_credit_orders WHERE status='PAID' AND paid_at>=? AND paid_at<?),
              (SELECT count(*) FROM ai_credit_orders WHERE status='PAID' AND paid_at>=? AND paid_at<?),
              (SELECT coalesce(sum(amount),0) FROM ai_credit_orders WHERE status='PAID' AND paid_at>=? AND paid_at<?),
              (SELECT coalesce(sum(amount),0) FROM ai_credit_refunds WHERE status='COMPLETED' AND completed_at>=? AND completed_at<?),
              (SELECT coalesce(sum(amount) filter(where amount>0),0) FROM ai_credit_ledger WHERE created_at>=? AND created_at<?),
              (SELECT coalesce(-sum(amount) filter(where amount<0),0) FROM ai_credit_ledger WHERE created_at>=? AND created_at<?),
              (SELECT coalesce(sum(estimated_cost),0) FROM ai_usage_records WHERE completed_at>=? AND completed_at<?),
              (SELECT count(*) FROM ai_usage_records WHERE success AND completed_at>=? AND completed_at<?)
            """, (rs,n)-> { long revenue=rs.getLong(6), refunds=rs.getLong(7), successful=rs.getLong(11); BigDecimal cost=rs.getBigDecimal(10); long net=revenue-refunds; return new Summary(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),revenue,refunds,net,rs.getLong(8),rs.getLong(9),cost,cost.divide(BigDecimal.valueOf(Math.max(1, successful)),8,java.math.RoundingMode.HALF_UP),net>0?cost.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(net),2,java.math.RoundingMode.HALF_UP):null); },
                Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to),Timestamp.valueOf(from),Timestamp.valueOf(to));
    }
    public List<Funnel> funnel(LocalDateTime from, LocalDateTime to) { return jdbc.query("SELECT event_type,count(DISTINCT coalesce(user_id::text,session_id)) FROM metric_events WHERE event_type IN ('FORTUNE_PAGE_VIEW','FORTUNE_PROFILE_STARTED','FORTUNE_PROFILE_COMPLETED','CHART_GENERATED','AI_CHAT_STARTED','FREE_CREDIT_USED','PAYWALL_VIEWED','PRODUCT_SELECTED','CHECKOUT_STARTED','PAYMENT_COMPLETED','PAID_AI_USED','RETURN_VISIT') AND created_at>=? AND created_at<? GROUP BY event_type", (rs,n)->new Funnel(rs.getString(1),rs.getLong(2)),Timestamp.valueOf(from),Timestamp.valueOf(to)); }
    public record Summary(long newUsers,long chartUsers,long aiUsers,long payingUsers,long payments,long revenue,long refunds,long netRevenue,long grantedCredits,long usedCredits,BigDecimal aiCost,BigDecimal averageAiCost,BigDecimal aiCostRate) {}
    public record Funnel(String eventName,long users) {}
}
