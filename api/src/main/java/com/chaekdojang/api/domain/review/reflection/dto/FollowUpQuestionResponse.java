package com.chaekdojang.api.domain.review.reflection.dto;

import com.chaekdojang.api.domain.review.reflection.ReviewFollowUpQuestion;

import java.time.LocalDateTime;

public record FollowUpQuestionResponse(
        String question,
        boolean aiGenerated,
        boolean userEdited,
        boolean stale,
        String model,
        String promptVersion,
        LocalDateTime updatedAt
) {
    public static FollowUpQuestionResponse from(ReviewFollowUpQuestion value, boolean stale) {
        return new FollowUpQuestionResponse(
                value.getQuestion(),
                true,
                value.isUserEdited(),
                stale,
                value.getModel(),
                value.getPromptVersion(),
                value.getUpdatedAt());
    }
}
