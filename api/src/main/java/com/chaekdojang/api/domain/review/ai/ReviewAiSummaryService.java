package com.chaekdojang.api.domain.review.ai;

import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.review.ai.dto.ReviewAiSummaryResponse;
import com.chaekdojang.api.domain.review.ai.dto.ReviewAiSummaryUpdateRequest;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewAiSummaryService {

    private final ReviewRepository reviewRepository;
    private final ReviewAiSummaryRepository summaryRepository;
    private final ReviewAiSummaryJobRepository jobRepository;
    private final OpenAiReviewSummaryClient openAiClient;
    private final ReviewAiSummaryProperties properties;

    @Transactional
    public void enqueueForReview(Review review) {
        ReviewAiSummary summary = summaryRepository.findByReviewId(review.getId())
                .orElseGet(() -> summaryRepository.save(ReviewAiSummary.builder().review(review).build()));
        summary.markPending();
        jobRepository.save(ReviewAiSummaryJob.builder().review(review).build());
    }

    @Transactional
    public ReviewAiSummaryResponse getStatus(Long reviewId) {
        Review review = findVisibleOrOwnedReview(reviewId);
        return summaryRepository.findByReviewId(review.getId())
                .filter(summary -> hasActiveOrFinishedSummary(review.getId(), summary))
                .map(ReviewAiSummaryResponse::from)
                .orElse(null);
    }

    @Transactional
    public ReviewAiSummaryResponse update(Long reviewId, ReviewAiSummaryUpdateRequest request) {
        Review review = findOwnedReview(reviewId);
        ReviewAiSummary summary = getOrCreateSummary(review);
        summary.edit(toResult(request));
        return ReviewAiSummaryResponse.from(summary);
    }

    @Transactional
    public ReviewAiSummaryResponse regenerate(Long reviewId) {
        Review review = findOwnedReview(reviewId);
        enqueueForReview(review);
        return ReviewAiSummaryResponse.from(getOrCreateSummary(review));
    }

    @Transactional
    public void processPendingJobs() {
        if (!properties.isEnabled()) return;
        List<ReviewAiSummaryJob> jobs = jobRepository.findByStatusOrderByIdAsc(
                ReviewAiSummaryStatus.PENDING,
                PageRequest.of(0, Math.max(1, properties.getBatchSize()))
        );
        for (ReviewAiSummaryJob job : jobs) {
            processJob(job.getId());
        }
    }

    @Transactional
    public void processJob(Long jobId) {
        int locked = jobRepository.updateStatusIfCurrent(
                jobId,
                ReviewAiSummaryStatus.PENDING,
                ReviewAiSummaryStatus.PROCESSING,
                LocalDateTime.now()
        );
        if (locked == 0) return;

        ReviewAiSummaryJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Review review = job.getReview();
        ReviewAiSummary summary = getOrCreateSummary(review);
        summary.markProcessing();

        try {
            AiSummaryResult result = openAiClient.summarize(review.getContent());
            summary.complete(result, job.getRetryCount());
            LocalDateTime now = LocalDateTime.now();
            jobRepository.finishJob(jobId, ReviewAiSummaryStatus.COMPLETED, job.getRetryCount(), null, now, now);
        } catch (Exception e) {
            handleFailure(job, summary, e);
        }
    }

    private void handleFailure(ReviewAiSummaryJob job, ReviewAiSummary summary, Exception e) {
        int nextRetryCount = job.getRetryCount() + 1;
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (nextRetryCount >= properties.getMaxRetries()) {
            summary.fail(message, nextRetryCount);
            LocalDateTime now = LocalDateTime.now();
            jobRepository.finishJob(job.getId(), ReviewAiSummaryStatus.FAILED, nextRetryCount, truncate(message), now, now);
            return;
        }
        summary.markPending();
        summary.updateRetryCount(nextRetryCount, message);
        jobRepository.finishJob(job.getId(), ReviewAiSummaryStatus.PENDING, nextRetryCount, truncate(message), LocalDateTime.now(), null);
    }

    private ReviewAiSummary getOrCreateSummary(Review review) {
        return summaryRepository.findByReviewId(review.getId())
                .orElseGet(() -> summaryRepository.save(ReviewAiSummary.builder().review(review).build()));
    }

    private boolean hasActiveOrFinishedSummary(Long reviewId, ReviewAiSummary summary) {
        if (summary.getStatus() != ReviewAiSummaryStatus.PENDING) return true;
        boolean hasActiveJob = jobRepository.existsByReviewIdAndStatusIn(
                reviewId,
                List.of(ReviewAiSummaryStatus.PENDING, ReviewAiSummaryStatus.PROCESSING)
        );
        return hasActiveJob;
    }

    private Review findVisibleOrOwnedReview(Long reviewId) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (review.isHidden()
                && !review.isAuthor(currentUserId)
                && !SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private Review findOwnedReview(Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        return review;
    }

    private AiSummaryResult toResult(ReviewAiSummaryUpdateRequest request) {
        return new AiSummaryResult(
                request.oneLineReview().trim(),
                request.emotionKeywords().stream().map(String::trim).filter(v -> !v.isBlank()).distinct().toList(),
                request.recommendedFor().trim(),
                request.impressivePoint().trim()
        );
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) return value;
        return value.substring(0, 1000);
    }
}
