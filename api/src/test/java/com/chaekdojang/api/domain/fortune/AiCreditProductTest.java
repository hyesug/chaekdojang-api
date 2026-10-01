package com.chaekdojang.api.domain.fortune;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiCreditProductTest {
    @Test
    void 기간권은_정해진_질문권과_만료일을_가진다() {
        assertThat(AiCreditProduct.DAY_PASS.credits()).isEqualTo(5);
        assertThat(AiCreditProduct.DAY_PASS.validDays()).isEqualTo(1);
        assertThat(AiCreditProduct.MONTH_PASS.credits()).isEqualTo(60);
        assertThat(AiCreditProduct.MONTH_PASS.validDays()).isEqualTo(30);
    }

    @Test
    void 일반_묶음은_만료되지_않는다() {
        assertThat(AiCreditProduct.PACK_30.validDays()).isZero();
        assertThat(AiCreditProduct.PACK_30.price()).isEqualTo(15_900);
    }
}
