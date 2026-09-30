package com.chaekdojang.api.domain.fortune;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AiCostCalculator {
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private final AiCreditProperties properties;
    public AiCostCalculator(AiCreditProperties properties) { this.properties = properties; }
    public BigDecimal estimate(int input, int output, int cacheRead, int cacheWrite) {
        return price(input, properties.getInputCostPerMillionTokens())
                .add(price(output, properties.getOutputCostPerMillionTokens()))
                .add(price(cacheRead, properties.getCacheReadCostPerMillionTokens()))
                .add(price(cacheWrite, properties.getCacheWriteCostPerMillionTokens()))
                .setScale(8, RoundingMode.HALF_UP);
    }
    private BigDecimal price(int tokens, BigDecimal perMillion) { return perMillion.multiply(BigDecimal.valueOf(tokens)).divide(MILLION, 8, RoundingMode.HALF_UP); }
}
