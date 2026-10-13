package com.chaekdojang.api.domain.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 예약 발송 독후감을 시각이 되면 공개한다. 1분마다 확인한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewPublishScheduler {

    private final ReviewService reviewService;

    @Scheduled(fixedDelayString = "${app.review.publish-scheduler-delay-ms:60000}", initialDelay = 30000)
    public void publishDue() {
        for (Long id : reviewService.dueScheduledIds()) {
            try {
                // 글마다 트랜잭션. 실패한 글만 다음 주기에 다시 시도한다.
                reviewService.publishScheduled(id);
            } catch (RuntimeException e) {
                log.error("예약 독후감 공개 실패. reviewId={}", id, e);
            }
        }
    }
}
