package com.chaekdojang.api.domain.fortune;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AiCostCalculator {
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    /** 캐시 쓰기 배수 (입력 단가 대비). 5분 캐시 1.25배, 1시간 캐시 2배 */
    private static final BigDecimal WRITE_5M = new BigDecimal("1.25");
    private static final BigDecimal WRITE_1H = new BigDecimal("2");
    private final AiCreditProperties properties;
    public AiCostCalculator(AiCreditProperties properties) { this.properties = properties; }

    /** 모델 단가 (USD / 100만 토큰). 가격이 바뀌면 여기만 고친다 */
    private record Price(BigDecimal input, BigDecimal output, BigDecimal cacheRead) {}
    private static Price priceOf(FortuneAiModelTier tier) {
        return switch (tier) {
            case CLAUDE_SONNET -> new Price(new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("0.20"));
            case GPT_SOL -> new Price(new BigDecimal("2"), new BigDecimal("10"), new BigDecimal("0.10"));
            case CLAUDE_OPUS -> new Price(new BigDecimal("4"), new BigDecimal("20"), new BigDecimal("0.20"));
            case GPT_ASTRA -> new Price(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("1"));
        };
    }

    public BigDecimal estimate(int input, int output, int cacheRead, int cacheWrite) {
        return price(input, properties.getInputCostPerMillionTokens())
                .add(price(output, properties.getOutputCostPerMillionTokens()))
                .add(price(cacheRead, properties.getCacheReadCostPerMillionTokens()))
                .add(price(cacheWrite, properties.getCacheWriteCostPerMillionTokens()))
                .setScale(8, RoundingMode.HALF_UP);
    }
    public BigDecimal estimate(FortuneAiModelTier tier, int input, int output, int cacheRead, int cacheWrite) {
        return estimate(tier, input, output, cacheRead, cacheWrite, 0);
    }
    /** cacheWrite 는 전체 캐시 쓰기, cacheWrite1h 는 그중 1시간 캐시로 쓴 몫이다 */
    public BigDecimal estimate(FortuneAiModelTier tier, int input, int output, int cacheRead, int cacheWrite, int cacheWrite1h) {
        Price p = priceOf(tier);
        int write1h = Math.max(0, Math.min(cacheWrite1h, cacheWrite));
        return price(input, p.input()).add(price(output, p.output())).add(price(cacheRead, p.cacheRead()))
                .add(price(cacheWrite - write1h, p.input().multiply(WRITE_5M)))
                .add(price(write1h, p.input().multiply(WRITE_1H)))
                .setScale(8, RoundingMode.HALF_UP);
    }
    private BigDecimal price(int tokens, BigDecimal perMillion) { return perMillion.multiply(BigDecimal.valueOf(tokens)).divide(MILLION, 8, RoundingMode.HALF_UP); }
}
