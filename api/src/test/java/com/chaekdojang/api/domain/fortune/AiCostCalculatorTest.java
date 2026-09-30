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
}
