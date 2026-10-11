package com.chaekdojang.api.domain.lotto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class LottoFutureValidationPolicy {
    static final LocalDate FIRST_DRAW_DATE = LocalDate.of(2002, 12, 7);
    static final LocalTime DEFAULT_DRAW_TIME = LocalTime.of(20, 35);
    // S1·S2·S3: 15체계 후보 슬롯 득표 전략(화면명 전략 A·B·C). DB CHECK 제약과 같은 목록이어야 한다.
    static final List<String> MODELS = List.of("A", "D", "E", "F", "H", "S1", "S2", "S3");
    static final List<String> STRATEGY_MODELS = List.of("S1", "S2", "S3");

    private LottoFutureValidationPolicy() {
    }

    static LocalDate nextDrawDate(LocalDate today) {
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    }

    static int roundOf(LocalDate drawDate) {
        return (int) (ChronoUnit.DAYS.between(FIRST_DRAW_DATE, drawDate) / 7L) + 1;
    }

    static DrawSchedule drawSchedule(LocalDate drawDate) {
        return switch (roundOf(drawDate)) {
            case 1243 -> new DrawSchedule(LocalTime.of(21, 30), "official_schedule_override",
                    "동행복권 공지: 2026 아이치·나고야 아시안게임 중계로 방송 예정 21:30~21:35, 계산 기준 21:30 KST");
            case 1244 -> new DrawSchedule(LocalTime.of(21, 35), "official_schedule_override",
                    "동행복권 공지: 2026 아이치·나고야 아시안게임 중계로 방송 예정 21:35~21:40, 계산 기준 21:35 KST");
            default -> new DrawSchedule(DEFAULT_DRAW_TIME, "official_default", "기본 추첨 시각 20:35 KST");
        };
    }

    // 정식 성적: 추첨 시각(KST) 이전에 생성·저장한 예측만 인정한다.
    static boolean isPreDraw(LocalDateTime generatedAt, LocalDate drawDate, LocalTime drawTime) {
        return generatedAt != null && generatedAt.isBefore(LocalDateTime.of(drawDate, drawTime));
    }

    static void requireTicket(List<Integer> numbers) {
        if (numbers == null || numbers.size() != 6 || numbers.stream().distinct().count() != 6
                || numbers.stream().anyMatch(number -> number == null || number < 1 || number > 45)) {
            throw new IllegalArgumentException("로또 번호는 1~45의 서로 다른 6개여야 합니다.");
        }
    }

    record DrawSchedule(LocalTime drawTime, String timeSource, String timeSourceDetail) {}
}
