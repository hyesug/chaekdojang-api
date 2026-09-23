package com.chaekdojang.api.domain.lotto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class LottoFutureValidationPolicy {
    static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);
    static final LocalTime DEFAULT_DRAW_TIME = LocalTime.of(20, 35);

    private LottoFutureValidationPolicy() {
    }

    static LocalDate nextDrawDate(LocalDate today) {
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    }

    static int roundOf(LocalDate drawDate) {
        return (int) (ChronoUnit.DAYS.between(FIRST_DRAW_DATE, drawDate) / 7L) + 1;
    }

    static void requireTicket(List<Integer> numbers) {
        if (numbers == null || numbers.size() != 6 || numbers.stream().distinct().count() != 6
                || numbers.stream().anyMatch(number -> number == null || number < 1 || number > 45)) {
            throw new IllegalArgumentException("로또 번호는 1~45의 서로 다른 6개여야 합니다.");
        }
    }
}
