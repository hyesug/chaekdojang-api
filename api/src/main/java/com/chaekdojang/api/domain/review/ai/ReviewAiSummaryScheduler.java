package com.chaekdojang.api.domain.review.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewAiSummaryScheduler {

    private final ReviewAiSummaryService summaryService;

    @Scheduled(fixedDelayString = "${app.ai-summary.scheduler-delay-ms:5000}")
    public void processPendingJobs() {
        summaryService.processPendingJobs();
    }
}
