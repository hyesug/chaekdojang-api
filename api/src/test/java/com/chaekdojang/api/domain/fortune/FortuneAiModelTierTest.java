package com.chaekdojang.api.domain.fortune;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FortuneAiModelTierTest {
    @Test
    void chargesCreditsByConfiguredModelCostTier() {
        assertThat(FortuneAiModelTier.CLAUDE_SONNET.creditCost()).isEqualTo(1);
        assertThat(FortuneAiModelTier.GPT_SOL.creditCost()).isEqualTo(1);
        assertThat(FortuneAiModelTier.CLAUDE_OPUS.creditCost()).isEqualTo(2);
        assertThat(FortuneAiModelTier.GPT_ASTRA.creditCost()).isEqualTo(5);
    }
}
