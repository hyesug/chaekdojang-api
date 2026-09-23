package com.chaekdojang.api.domain.lotto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface LottoFuturePredictionGenerator {
    Generated generate(int round, LocalDate drawDate, LocalTime drawTime, List<History> history);

    record History(int round, LocalDate drawDate, LocalTime drawTime, List<Integer> numbers) {}
    record Generated(Map<String, List<Integer>> predictions, String modelVersion, String sourceCommit) {}
}
