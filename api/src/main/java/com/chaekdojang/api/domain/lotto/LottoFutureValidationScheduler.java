package com.chaekdojang.api.domain.lotto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class LottoFutureValidationScheduler {
    private final LottoFutureValidationService lottoFutureValidationService;

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Seoul")
    void generateComingSaturday() {
        try {
            lottoFutureValidationService.generateNext();
        } catch (RuntimeException exception) {
            log.error("로또 미래검증 자동 생성을 완료하지 못했습니다.", exception);
        }
    }
}
