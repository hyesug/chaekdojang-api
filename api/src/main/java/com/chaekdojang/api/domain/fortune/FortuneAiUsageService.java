package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FortuneAiUsageService {

    private static final int MONTHLY_LIMIT = 5;

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    /**
     * 비용이 나는 Claude 호출 직전에 한 자리를 선점한다. 사용자별 advisory lock으로
     * 동시 탭 요청도 월 한도를 넘지 못하게 한다.
     */
    @Transactional
    public ReservationResponse reserve(Long userId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:userId)")
                .setParameter("userId", userId)
                .getSingleResult();

        LocalDate month = LocalDate.now().withDayOfMonth(1);
        LocalDateTime from = month.atStartOfDay();
        LocalDateTime until = month.plusMonths(1).atStartOfDay();
        Integer used = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fortune_ai_usages WHERE user_id = ? AND created_at >= ? AND created_at < ?",
                Integer.class, userId, from, until
        );
        if (used != null && used >= MONTHLY_LIMIT) {
            throw new CustomException(ErrorCode.FORTUNE_AI_LIMIT_EXCEEDED);
        }

        jdbcTemplate.update(
                "INSERT INTO fortune_ai_usages (id, user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), userId
        );
        int remaining = MONTHLY_LIMIT - (used == null ? 1 : used + 1);
        return new ReservationResponse(remaining, MONTHLY_LIMIT);
    }

    public record ReservationResponse(int remaining, int monthlyLimit) {}
}
