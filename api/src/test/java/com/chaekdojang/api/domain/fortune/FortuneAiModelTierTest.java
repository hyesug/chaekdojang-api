package com.chaekdojang.api.domain.fortune;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FortuneAiModelTierTest {
    @Test
    void chargesCreditsByConfiguredModelCostTier() {
        assertThat(FortuneAiModelTier.CLAUDE_SONNET.creditCost()).isEqualTo(1);
        assertThat(FortuneAiModelTier.GPT_SOL.creditCost()).isEqualTo(1);
        assertThat(FortuneAiModelTier.CLAUDE_OPUS.creditCost()).isEqualTo(1);   // 운세 AI 고정 모델 — 질문 한 번에 1회
        assertThat(FortuneAiModelTier.GPT_ASTRA.creditCost()).isEqualTo(5);
    }
}
