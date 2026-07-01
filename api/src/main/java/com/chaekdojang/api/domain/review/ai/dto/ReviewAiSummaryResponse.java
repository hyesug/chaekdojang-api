package com.chaekdojang.api.domain.review.ai.dto;

import com.chaekdojang.api.domain.review.ai.ReviewAiSummary;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummarySource;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewAiSummaryResponse(
        Long reviewId,
        String oneLineReview,
        List<String> emotionKeywords,
        String recommendedFor,
        String impressivePoint,
        ReviewAiSummaryStatus status,
        int retryCount,
        String errorMessage,
        ReviewAiSummarySource summarySource,
        boolean userEdited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static ReviewAiSummaryResponse from(ReviewAiSummary summary) {
        return new ReviewAiSummaryResponse(
                summary.getReview().getId(),
                summary.getOneLineReview(),
                List.copyOf(summary.getEmotionKeywords()),
                summary.getRecommendedFor(),
                summary.getImpressivePoint(),
                summary.getStatus(),
                summary.getRetryCount(),
                summary.getErrorMessage(),
                summary.getSummarySource(),
                summary.isUserEdited(),
                summary.getCreatedAt(),
                summary.getUpdatedAt(),
                summary.getCompletedAt()
        );
    }
}
