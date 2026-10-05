package com.chaekdojang.api.domain.fortune;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class AiCostCalculatorTest {
    @Test
    void usesSeparateProviderTokenPricesWithoutReplacingReportedUsage() {
        AiCreditProperties properties = new AiCreditProperties();
        BigDecimal cost = new AiCostCalculator(properties).estimate(1_000_000, 1_000_000, 1_000_000, 1_000_000);
        assertThat(cost).isEqualByComparingTo("36.75000000");
    }

    @Test
    void tierPricesStayTheSameForFiveMinuteCacheWrites() {
        AiCostCalculator calc = new AiCostCalculator(new AiCreditProperties());
        // Sonnet: 입력 2 + 출력 10 + 캐시읽기 0.20 + 5분 캐시쓰기 2.50
        assertThat(calc.estimate(FortuneAiModelTier.CLAUDE_SONNET, 1_000_000, 1_000_000, 1_000_000, 1_000_000))
                .isEqualByComparingTo("14.70000000");
        // Opus: 4 + 20 + 0.20 + 5
        assertThat(calc.estimate(FortuneAiModelTier.CLAUDE_OPUS, 1_000_000, 1_000_000, 1_000_000, 1_000_000))
                .isEqualByComparingTo("29.20000000");
    }

    @Test
    void oneHourCacheWritesCostTwiceTheInputPrice() {
        AiCostCalculator calc = new AiCostCalculator(new AiCreditProperties());
        // Opus 캐시쓰기 100만 중 60만이 1시간: 40만×5 + 60만×8 = 2 + 4.8
        assertThat(calc.estimate(FortuneAiModelTier.CLAUDE_OPUS, 0, 0, 0, 1_000_000, 600_000))
                .isEqualByComparingTo("6.80000000");
        // 1시간 몫이 전체보다 크게 와도 전체를 넘겨 셈하지 않는다
        assertThat(calc.estimate(FortuneAiModelTier.CLAUDE_SONNET, 0, 0, 0, 100, 1_000))
                .isEqualByComparingTo("0.00040000");
    }
}
