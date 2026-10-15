package com.chaekdojang.api.domain.lotto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface LottoFuturePredictionGenerator {
    Generated generate(int round, LocalDate drawDate, LocalTime drawTime, List<History> history);

    // 변동 순위표: 후보 규칙 전부의 번호와 그 시점 순위. results 는 이전 회차에 저장한 순위표 번호와 당첨번호다.
    Leaderboard generateLeaderboard(int round, LocalDate drawDate, LocalTime drawTime, List<History> history, List<LeaderboardResult> results);

    record History(int round, LocalDate drawDate, LocalTime drawTime, List<Integer> numbers) {}
    record Generated(Map<String, List<Integer>> predictions, String modelVersion, String sourceCommit, String engineVersion) {}
    record LeaderboardResult(int round, List<Integer> actual, Map<String, List<Integer>> tickets) {}
    record LeaderboardEntry(String ruleId, List<Integer> numbers, Integer rank, Double score, int recordRounds,
                            Double recordMean, Double recentMean, Integer top5Rank) {}
    record Leaderboard(int round, String modelVersion, String engineVersion, List<LeaderboardEntry> entries) {}
}
