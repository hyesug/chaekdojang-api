package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 관리자 'AI 질문권' 회원 목록 — 검색어·페이지 인자가 SQL 에 바르게 들어가는지 */
class AiCreditAdminUserListTest {

    private AiCreditService service(JdbcTemplate jdbc) {
        return new AiCreditService(jdbc, new AiCreditProperties(), new AiCostCalculator(new AiCreditProperties()),
                mock(UserRepository.class), mock(AdminAuditLogService.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchMatchesNicknameEmailOrIdAndClampsPaging() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM users u"), eq(Long.class), any(Object[].class))).thenReturn(3L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        AiCreditService.UserPage page = service(jdbc).userList("  kim ", -2, 999);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), args.capture());
        assertThat(sql.getValue()).contains("u.nickname ILIKE ?").contains("ORDER BY a.last_used_at DESC NULLS LAST").contains("WHERE u.deleted_at IS NULL");
        assertThat(args.getValue()).containsExactly("%kim%", "%kim%", "kim", 100, 0L);
        assertThat(page.total()).isEqualTo(3L);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptySearchHasNoFilter() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        service(jdbc).userList(null, 2, 30);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), args.capture());
        assertThat(sql.getValue()).doesNotContain("ILIKE");
        assertThat(args.getValue()).containsExactly(30, 60L);
    }
}
