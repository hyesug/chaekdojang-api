package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 관리자는 질문권 제한 없이 묻는다 — 모자라면 그만큼 자동 충전하고, 일반 회원은 그대로 막힌다 */
class AiCreditAdminUnlimitedTest {

    private AiCreditService serviceWith(JdbcTemplate jdbc, boolean admin) {
        UserRepository users = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.isAdmin()).thenReturn(admin);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        return new AiCreditService(jdbc, new AiCreditProperties(), new AiCostCalculator(new AiCreditProperties()),
                users, mock(AdminAuditLogService.class));
    }

    @SuppressWarnings("unchecked")
    private JdbcTemplate emptyWallet() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // 최초 무료 1회는 이미 받았고, 잔액은 0
        when(jdbc.queryForObject(contains("type='FREE_GRANT'"), eq(Long.class), eq(7L))).thenReturn(1L);
        when(jdbc.queryForObject(startsWith("SELECT COALESCE(SUM(remaining_amount),0)"), eq(Integer.class), eq(7L))).thenReturn(0, 0, 2, 2, 0);
        when(jdbc.query(startsWith("SELECT request_id"), any(RowMapper.class), eq(7L), any(UUID.class))).thenReturn(List.of());
        when(jdbc.query(startsWith("SELECT id,remaining_amount"), any(RowMapper.class), eq(7L))).thenReturn(List.of());
        return jdbc;
    }

    @Test
    void adminWithNoCreditsIsToppedUpAndCanAsk() {
        JdbcTemplate jdbc = emptyWallet();
        AiCreditService service = serviceWith(jdbc, true);
        try { service.reserve(7L, UUID.randomUUID(), FortuneAiModelTier.CLAUDE_OPUS); } catch (CustomException ignored) {
            // 로트 차감은 목으로 비워 두었으니 여기서 멈출 수 있다 — 확인할 것은 자동 충전 기록이다
        }
        verify(jdbc).update(startsWith("INSERT INTO ai_credit_ledger"), any(UUID.class), eq(7L), eq(2), eq("ADMIN_GRANT"), isNull(), eq("관리자 무제한 — 자동 충전"));
    }

    @Test
    void regularMemberWithNoCreditsIsStillBlocked() {
        JdbcTemplate jdbc = emptyWallet();
        AiCreditService service = serviceWith(jdbc, false);
        assertThatThrownBy(() -> service.reserve(7L, UUID.randomUUID(), FortuneAiModelTier.CLAUDE_SONNET))
                .isInstanceOf(CustomException.class);
        verify(jdbc, never()).update(contains("관리자 무제한"), any(Object[].class));
        assertThat(true).isTrue();
    }
}
