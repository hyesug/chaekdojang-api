package com.chaekdojang.api.domain.lotto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
class HttpLottoFuturePredictionGenerator implements LottoFuturePredictionGenerator {
    private final String generatorUrl;
    private final String leaderboardUrl;
    private final String token;
    private final RestClient restClient;

    HttpLottoFuturePredictionGenerator(
            @Value("${app.lotto-future.generator-url:}") String generatorUrl,
            @Value("${app.lotto-future.leaderboard-url:}") String leaderboardUrl,
            @Value("${app.lotto-future.generator-token:}") String token,
            RestClient.Builder restClientBuilder) {
        this.generatorUrl = generatorUrl;
        // 비어 있으면 같은 웹의 순위표 엔드포인트를 쓴다(.../lotto-future-predictions → .../lotto-future-leaderboard).
        this.leaderboardUrl = leaderboardUrl.isBlank() ? generatorUrl.replace("lotto-future-predictions", "lotto-future-leaderboard") : leaderboardUrl;
        this.token = token;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Generated generate(int round, LocalDate drawDate, LocalTime drawTime, List<History> history) {
        Generated result = post(generatorUrl, new GenerateRequest(round, drawDate, drawTime, history), Generated.class);
        if (result == null) throw new IllegalStateException("로또 미래검증 계산 결과가 비어 있습니다.");
        return result;
    }

    @Override
    public Leaderboard generateLeaderboard(int round, LocalDate drawDate, LocalTime drawTime, List<History> history, List<LeaderboardResult> results) {
        Leaderboard result = post(leaderboardUrl, new LeaderboardRequest(round, drawDate, drawTime, history, results), Leaderboard.class);
        if (result == null) throw new IllegalStateException("로또 순위표 계산 결과가 비어 있습니다.");
        return result;
    }

    private <T> T post(String url, Object body, Class<T> type) {
        if (url.isBlank() || token.isBlank()) {
            throw new IllegalStateException("로또 미래검증 계산 엔드포인트가 설정되지 않았습니다.");
        }
        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Lotto-Prediction-Token", token)
                .body(body)
                .retrieve()
                .body(type);
    }

    private record GenerateRequest(int round, LocalDate drawDate, LocalTime drawTime, List<History> history) {}
    private record LeaderboardRequest(int round, LocalDate drawDate, LocalTime drawTime, List<History> history, List<LeaderboardResult> results) {}
}
