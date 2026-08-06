package com.chaekdojang.api.domain.review.reflection.dto;

import com.chaekdojang.api.domain.review.reflection.ReviewChangeComparison;

import java.time.LocalDateTime;

public record ChangeComparisonResponse(
        Long previousReviewId,
        String previousFocus,
        String currentFocus,
        String sharedThought,
        String changedPerspective,
        String newElement,
        String reflectionQuestion,
        boolean aiGenerated,
        boolean userEdited,
        boolean stale,
        String model,
        String promptVersion,
        LocalDateTime updatedAt
) {
    public static ChangeComparisonResponse from(ReviewChangeComparison value, boolean stale) {
        return new ChangeComparisonResponse(
                value.getPreviousReview().getId(),
                value.getPreviousFocus(), value.getCurrentFocus(), value.getSharedThought(),
                value.getChangedPerspective(), value.getNewElement(), value.getReflectionQuestion(),
                true, value.isUserEdited(), stale,
                value.getModel(), value.getPromptVersion(), value.getUpdatedAt());
    }
}
