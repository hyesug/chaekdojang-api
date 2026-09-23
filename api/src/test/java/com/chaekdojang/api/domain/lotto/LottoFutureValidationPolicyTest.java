package com.chaekdojang.api.domain.lotto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LottoFutureValidationPolicyTest {

    @Test
    void mondayResolvesTheComingSaturdayAtTheOfficialDefaultTime() {
        LocalDate drawDate = LottoFutureValidationPolicy.nextDrawDate(LocalDate.of(2026, 9, 21));

        assertThat(drawDate).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(LottoFutureValidationPolicy.DEFAULT_DRAW_TIME).isEqualTo(LocalTime.of(20, 35));
        assertThat(LottoFutureValidationPolicy.roundOf(drawDate)).isEqualTo(1243);
    }

    @Test
    void appliesTheOfficialAsianGamesBroadcastOverridesForRounds1243And1244() {
        LottoFutureValidationPolicy.DrawSchedule round1243 = LottoFutureValidationPolicy.drawSchedule(LocalDate.of(2026, 9, 26));
        LottoFutureValidationPolicy.DrawSchedule round1244 = LottoFutureValidationPolicy.drawSchedule(LocalDate.of(2026, 10, 3));

        assertThat(round1243.drawTime()).isEqualTo(LocalTime.of(21, 30));
        assertThat(round1244.drawTime()).isEqualTo(LocalTime.of(21, 35));
        assertThat(round1243.timeSource()).isEqualTo("official_schedule_override");
        assertThat(round1244.timeSource()).isEqualTo("official_schedule_override");
    }

    @Test
    void ticketMustHaveSixDistinctNumbersInRange() {
        LottoFutureValidationPolicy.requireTicket(List.of(1, 7, 11, 22, 34, 45));

        assertThatThrownBy(() -> LottoFutureValidationPolicy.requireTicket(List.of(1, 1, 2, 3, 4, 5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LottoFutureValidationPolicy.requireTicket(List.of(1, 2, 3, 4, 5, 46)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
