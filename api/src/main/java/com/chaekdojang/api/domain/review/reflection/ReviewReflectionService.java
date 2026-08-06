package com.chaekdojang.api.domain.review.reflection;

import com.chaekdojang.api.domain.feedback.FeedbackProperties;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryProperties;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryRepository;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryStatus;
import com.chaekdojang.api.domain.review.reflection.dto.*;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewReflectionService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReviewRepository reviewRepository;
    private final ReviewAiSummaryRepository reviewAiSummaryRepository;
    private final ReviewFollowUpQuestionRepository followUpRepository;
    private final ReviewChangeComparisonRepository comparisonRepository;
    private final ReviewReflectionAiClient aiClient;
    private final ReviewAiSummaryProperties aiProperties;
    private final FeedbackProperties feedbackProperties;
    private final MetricEventRepository metricEventRepository;
    private final MetricEventService metricEventService;

    public FollowUpQuestionResponse getFollowUp(Long reviewId) {
        Review review = ownedReview(reviewId);
        String hash = followUpHash(review);
        return followUpRepository.findByReviewId(reviewId)
                .map(value -> FollowUpQuestionResponse.from(value, !value.getSourceHash().equals(hash)))
                .orElse(null);
    }

    @Transactional
    public FollowUpQuestionResponse generateFollowUp(Long reviewId, boolean regenerate) {
        Review review = ownedReviewForUpdate(reviewId);
        validateEnabled();
        String cardContext = existingCardContext(reviewId);
        String sourceHash = hash(review.getContent() + "\n---AI-CARD---\n" + cardContext);
        ReviewFollowUpQuestion existing = followUpRepository.findByReviewId(reviewId).orElse(null);
        if (!regenerate && existing != null && existing.getSourceHash().equals(sourceHash)) {
            return FollowUpQuestionResponse.from(existing, false);
        }
        assertDailyLimit(review.getAuthor().getId());
        try {
            FollowUpQuestionResult result = aiClient.createFollowUpQuestion(
                    clipped(review.getContent()), cardContext);
            ReviewFollowUpQuestion value;
            if (existing == null) {
                value = followUpRepository.save(ReviewFollowUpQuestion.create(
                        review, result.question(), sourceHash, aiProperties.getModel(),
                        ReviewReflectionAiClient.FOLLOW_UP_PROMPT_VERSION));
            } else {
                existing.applyAi(result.question(), sourceHash, aiProperties.getModel(),
                        ReviewReflectionAiClient.FOLLOW_UP_PROMPT_VERSION);
                value = existing;
            }
            recordSuccess(reviewId, review.getAuthor().getId(), "follow_up",
                    ReviewReflectionAiClient.FOLLOW_UP_PROMPT_VERSION,
                    result.inputTokens(), result.outputTokens());
            return FollowUpQuestionResponse.from(value, false);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("review follow-up generation failed: reviewId={}", reviewId, e);
            recordFailure(reviewId, review.getAuthor().getId(), "follow_up", e);
            throw new CustomException(ErrorCode.AI_REFLECTION_FAILED);
        }
    }

    @Transactional
    public FollowUpQuestionResponse updateFollowUp(Long reviewId, FollowUpQuestionUpdateRequest request) {
        Review review = ownedReview(reviewId);
        ReviewFollowUpQuestion value = followUpRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLLOW_UP_QUESTION_NOT_FOUND));
        value.edit(request.question().trim());
        return FollowUpQuestionResponse.from(value, !value.getSourceHash().equals(followUpHash(review)));
    }

    @Transactional
    public void deleteFollowUp(Long reviewId) {
        ownedReview(reviewId);
        followUpRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLLOW_UP_QUESTION_NOT_FOUND));
        followUpRepository.deleteByReviewId(reviewId);
    }

    public ChangeComparisonResponse getComparison(Long reviewId) {
        Review review = ownedReview(reviewId);
        Review previous = comparablePrevious(review);
        String hash = comparisonHash(previous, review);
        return comparisonRepository.findByReviewId(reviewId)
                .map(value -> ChangeComparisonResponse.from(value, !value.getSourceHash().equals(hash)))
                .orElse(null);
    }

    @Transactional
    public ChangeComparisonResponse generateComparison(Long reviewId, boolean regenerate) {
        Review review = ownedReviewForUpdate(reviewId);
        Review previous = comparablePrevious(review);
        validateEnabled();
        String sourceHash = comparisonHash(previous, review);
        ReviewChangeComparison existing = comparisonRepository.findByReviewId(reviewId).orElse(null);
        if (!regenerate && existing != null && existing.getSourceHash().equals(sourceHash)) {
            return ChangeComparisonResponse.from(existing, false);
        }
        assertDailyLimit(review.getAuthor().getId());
        try {
            ChangeComparisonResult result = aiClient.compare(
                    clipped(previous.getContent()), clipped(review.getContent()));
            ReviewChangeComparison value;
            if (existing == null) {
                value = comparisonRepository.save(ReviewChangeComparison.create(
                        review, previous, result, sourceHash, aiProperties.getModel(),
                        ReviewReflectionAiClient.COMPARISON_PROMPT_VERSION));
            } else {
                existing.applyAi(result, sourceHash, aiProperties.getModel(),
                        ReviewReflectionAiClient.COMPARISON_PROMPT_VERSION);
                value = existing;
            }
            recordSuccess(reviewId, review.getAuthor().getId(), "change_comparison",
                    ReviewReflectionAiClient.COMPARISON_PROMPT_VERSION,
                    result.inputTokens(), result.outputTokens());
            return ChangeComparisonResponse.from(value, false);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("review comparison generation failed: reviewId={}", reviewId, e);
            recordFailure(reviewId, review.getAuthor().getId(), "change_comparison", e);
            throw new CustomException(ErrorCode.AI_REFLECTION_FAILED);
        }
    }

    @Transactional
    public ChangeComparisonResponse updateComparison(Long reviewId, ChangeComparisonUpdateRequest request) {
        Review review = ownedReview(reviewId);
        Review previous = comparablePrevious(review);
        ReviewChangeComparison value = comparisonRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANGE_COMPARISON_NOT_FOUND));
        value.edit(request.toResult());
        return ChangeComparisonResponse.from(value,
                !value.getSourceHash().equals(comparisonHash(previous, review)));
    }

    @Transactional
    public void deleteComparison(Long reviewId) {
        ownedReview(reviewId);
        comparisonRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANGE_COMPARISON_NOT_FOUND));
        comparisonRepository.deleteByReviewId(reviewId);
    }

    private Review ownedReview(Long reviewId) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isAuthor(SecurityUtils.getCurrentUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return review;
    }

    private Review ownedReviewForUpdate(Long reviewId) {
        Review review = reviewRepository.findForUpdateByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isAuthor(SecurityUtils.getCurrentUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return review;
    }

    private Review comparablePrevious(Review review) {
        Review previous = review.getPreviousReview();
        if (previous == null
                || previous.getDeletedAt() != null
                || previous.getBook() == null || review.getBook() == null
                || !previous.getBook().getId().equals(review.getBook().getId())
                || !previous.getAuthor().getId().equals(review.getAuthor().getId())) {
            throw new CustomException(ErrorCode.CHANGE_COMPARISON_NOT_AVAILABLE);
        }
        return previous;
    }

    private void validateEnabled() {
        if (!aiProperties.isEnabled()
                || aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new CustomException(ErrorCode.AI_REFLECTION_DISABLED);
        }
    }

    private void assertDailyLimit(Long userId) {
        int limit = feedbackProperties.getMemberDailyLimit();
        if (limit <= 0) return;
        var todayStart = LocalDate.now(KST).atStartOfDay();
        long used = List.of("feedback_succeeded", "reflection_ai_succeeded",
                        "reading_group_ai_question_succeeded", "reading_group_ai_analysis_succeeded")
                .stream().mapToLong(eventType -> metricEventRepository
                        .countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(
                                userId, eventType, todayStart))
                .sum();
        if (used >= limit) throw new CustomException(ErrorCode.AI_REFLECTION_LIMIT_EXCEEDED);
    }

    private void recordSuccess(
            Long reviewId, Long userId, String insightType, String promptVersion,
            long inputTokens, long outputTokens) {
        metricEventService.recordCurrentRequestEvent(
                "reflection_ai_succeeded", userId, "/reviews/" + reviewId,
                Map.of("reviewId", reviewId, "insightType", insightType,
                        "model", aiProperties.getModel(), "promptVersion", promptVersion,
                        "inputTokens", inputTokens, "outputTokens", outputTokens,
                        "estimatedCostUsd", estimatedCostUsd(inputTokens, outputTokens)));
    }

    private void recordFailure(Long reviewId, Long userId, String insightType, Exception error) {
        metricEventService.recordCurrentRequestEvent(
                "reflection_ai_failed", userId, "/reviews/" + reviewId,
                Map.of("reviewId", reviewId, "insightType", insightType,
                        "reason", error.getClass().getSimpleName()));
    }

    private String clipped(String content) {
        String value = content == null ? "" : content.trim();
        int limit = feedbackProperties.getMaxChars() > 0 ? feedbackProperties.getMaxChars() : 6000;
        return value.substring(0, Math.min(value.length(), limit));
    }

    private String comparisonHash(Review previous, Review current) {
        return hash(previous.getContent() + "\n---CURRENT---\n" + current.getContent());
    }

    private String followUpHash(Review review) {
        return hash(review.getContent() + "\n---AI-CARD---\n" + existingCardContext(review.getId()));
    }

    private String existingCardContext(Long reviewId) {
        return reviewAiSummaryRepository.findByReviewId(reviewId)
                .filter(summary -> summary.getStatus() == ReviewAiSummaryStatus.COMPLETED
                        || summary.getStatus() == ReviewAiSummaryStatus.EDITED)
                .map(summary -> "한 줄 감상: " + nullToEmpty(summary.getOneLineReview())
                        + "\n감정 키워드: " + String.join(", ", summary.getEmotionKeywords())
                        + "\n인상적인 지점: " + nullToEmpty(summary.getImpressivePoint()))
                .orElse("");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double estimatedCostUsd(long inputTokens, long outputTokens) {
        double cost = inputTokens * aiProperties.getInputCostPerMillionTokens() / 1_000_000d
                + outputTokens * aiProperties.getOutputCostPerMillionTokens() / 1_000_000d;
        return Math.round(cost * 100_000_000d) / 100_000_000d;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }
}
