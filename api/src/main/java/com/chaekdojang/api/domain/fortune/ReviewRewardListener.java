package com.chaekdojang.api.domain.fortune;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 독후감을 쓰면 AI 질문권 1개 — 결제를 붙이기 전까지 질문권을 얻는 길.
 *
 * 독후감 저장이 **확정된 뒤**(AFTER_COMMIT)에 준다. 지급이 실패해도 독후감 저장은 그대로 둔다 —
 * 질문권 때문에 글이 날아가면 안 된다. 실패는 기록만 하고 넘긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRewardListener {

    private final AiCreditService aiCreditService;

    /** 독후감이 새로 저장됐거나 고쳐졌다 */
    public record ReviewWritten(Long userId, Long reviewId, Long bookId, String content) {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReviewWritten(ReviewWritten event) {
        try {
            aiCreditService.grantReviewReward(event.userId(), event.reviewId(), event.bookId(), event.content());
        } catch (RuntimeException e) {
            log.warn("[review-reward] user={} review={} 질문권 지급 실패: {}", event.userId(), event.reviewId(), e.toString());
        }
    }
}
