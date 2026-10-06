package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 호출 실패 복구(refund)가 남기는 실패 기록 INSERT 의 열과 값 자리를 맞춰 본다.
 * 예전에는 success 자리에 오류 문자열이, error_type 자리에 false 가 들어가 PostgreSQL 이
 * 거절했고, 그 바람에 질문권 복구 트랜잭션 전체가 롤백돼 실패한 질문의 질문권이 돌아오지 않았다.
 * (H2 테스트 DB 로는 원장 SQL 을 돌릴 수 없어 SQL 문장 자체를 검사한다.)
 */
class AiCreditServiceRefundSqlTest {

    @Test
    void refundFailureRecordPutsFalseInSuccessAndErrorTypeInErrorType() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AiCreditService service = new AiCreditService(jdbc, new AiCreditProperties(),
                new AiCostCalculator(new AiCreditProperties()), mock(UserRepository.class), mock(AdminAuditLogService.class));
        UUID requestId = UUID.randomUUID();
        AiCreditService.Reservation reserved = new AiCreditService.Reservation(requestId, 0, 0, "RESERVED",
                LocalDateTime.now().minusSeconds(3), FortuneAiModelTier.CLAUDE_OPUS, 2);
        doReturn(List.of(reserved)).when(jdbc).query(startsWith("SELECT request_id"), any(RowMapper.class), eq(7L), eq(requestId));

        service.refund(7L, requestId, "OVERLOADED");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, atLeastOnce()).update(sql.capture(), args.capture());
        int i = sql.getAllValues().indexOf(sql.getAllValues().stream().filter(s -> s.startsWith("INSERT INTO ai_usage_records")).findFirst().orElseThrow());
        String insert = sql.getAllValues().get(i);
        Object[] bound = flatten(args.getAllValues().get(i));

        List<String> columns = Arrays.stream(between(insert, "(", ")").split(",")).map(String::trim).toList();
        List<String> values = Arrays.stream(between(insert.substring(insert.indexOf("VALUES")), "(", ")").split(",")).map(String::trim).toList();
        assertThat(values).hasSameSizeAs(columns);
        assertThat(values.stream().filter("?"::equals).count()).isEqualTo(bound.length);

        assertThat(values.get(columns.indexOf("success"))).isEqualTo("false");
        int errorTypeSlot = (int) values.subList(0, columns.indexOf("error_type") + 1).stream().filter("?"::equals).count() - 1;
        assertThat(values.get(columns.indexOf("error_type"))).isEqualTo("?");
        assertThat(bound[errorTypeSlot]).isEqualTo("OVERLOADED");
    }

    private static String between(String s, String open, String close) {
        int a = s.indexOf(open);
        return s.substring(a + 1, s.indexOf(close, a));
    }

    /** Mockito 가 가변 인자를 한 배열로 줄 때와 낱개로 줄 때를 모두 받는다 */
    private static Object[] flatten(Object[] raw) {
        List<Object> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Object[] nested) out.addAll(Arrays.asList(nested));
            else out.add(o);
        }
        return out.toArray();
    }
}
