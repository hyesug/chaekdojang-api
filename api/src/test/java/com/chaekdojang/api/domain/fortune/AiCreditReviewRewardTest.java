package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // users 잠금 조회는 값이 상관없어 따로 맞추지 않는다
class AiCreditReviewRewardTest {

    @Mock JdbcTemplate jdbc;
    @Mock AiCreditProperties properties;
    @Mock AiCostCalculator costCalculator;
    @Mock UserRepository userRepository;
    @Mock AdminAuditLogService auditLogService;
    @InjectMocks AiCreditService service;

    private static final String LONG = "가".repeat(AiCreditService.REVIEW_REWARD_MIN_CHARS);

    @Test
    void grantsOneCreditForAFirstReviewOfABook() {
        when(jdbc.queryForObject(contains("FROM ai_credit_review_rewards"), eq(Integer.class), eq(7L), eq(30L))).thenReturn(0);

        assertThat(service.grantReviewReward(7L, 100L, 30L, LONG)).isTrue();

        verify(jdbc).update(contains("INSERT INTO ai_credit_ledger"), any(), eq(7L), eq(1), eq("REVIEW_REWARD"), isNull(), contains("100"));
        verify(jdbc).update(contains("INSERT INTO ai_credit_review_rewards"), eq(7L), eq(30L), eq(100L), any());
        verify(jdbc).update(contains("INSERT INTO ai_credit_lots"), any(), eq(7L), any(), eq("REVIEW_REWARD"), eq(1), eq(1), isNull());
    }

    @Test
    void doesNotGrantTwiceForTheSameBook() {
        when(jdbc.queryForObject(contains("FROM ai_credit_review_rewards"), eq(Integer.class), eq(7L), eq(30L))).thenReturn(1);

        assertThat(service.grantReviewReward(7L, 101L, 30L, LONG)).isFalse();

        verify(jdbc, never()).update(contains("INSERT INTO ai_credit_ledger"), any(Object[].class));
    }

    @Test
    void skipsReviewsWithoutABookOrShorterThanTheMinimum() {
        assertThat(service.grantReviewReward(7L, 102L, null, LONG)).isFalse();
        // 공백·줄바꿈은 한 칸으로 줄여 센다 — 띄어쓰기로 늘린 글은 못 넘는다
        String padded = "가 ".repeat(40) + "\n\n\n" + " ".repeat(200);
        assertThat(AiCreditService.reviewTextLength(padded)).isLessThan(AiCreditService.REVIEW_REWARD_MIN_CHARS);
        assertThat(service.grantReviewReward(7L, 103L, 30L, padded)).isFalse();
        verifyNoInteractions(jdbc);
    }
}
