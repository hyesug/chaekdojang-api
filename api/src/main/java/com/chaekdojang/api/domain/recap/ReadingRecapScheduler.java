package com.chaekdojang.api.domain.recap;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadingRecapScheduler {
    private final ReadingRecapService recapService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateDueIssues() {
        recapService.generateDueIssues();
    }
}
