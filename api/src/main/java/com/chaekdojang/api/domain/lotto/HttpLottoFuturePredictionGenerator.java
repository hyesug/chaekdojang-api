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
    private final String token;
    private final RestClient restClient;

    HttpLottoFuturePredictionGenerator(
            @Value("${app.lotto-future.generator-url:}") String generatorUrl,
            @Value("${app.lotto-future.generator-token:}") String token,
            RestClient.Builder restClientBuilder) {
        this.generatorUrl = generatorUrl;
        this.token = token;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Generated generate(int round, LocalDate drawDate, LocalTime drawTime, List<History> history) {
        if (generatorUrl.isBlank() || token.isBlank()) {
            throw new IllegalStateException("로또 미래검증 계산 엔드포인트가 설정되지 않았습니다.");
        }
        Generated result = restClient.post()
                .uri(generatorUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Lotto-Prediction-Token", token)
                .body(new GenerateRequest(round, drawDate, drawTime, history))
                .retrieve()
                .body(Generated.class);
        if (result == null) throw new IllegalStateException("로또 미래검증 계산 결과가 비어 있습니다.");
        return result;
    }

    private record GenerateRequest(int round, LocalDate drawDate, LocalTime drawTime, List<History> history) {}
}
