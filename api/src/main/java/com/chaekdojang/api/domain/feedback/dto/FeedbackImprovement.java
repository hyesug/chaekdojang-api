package com.chaekdojang.api.domain.feedback.dto;

public record FeedbackImprovement(
        String point,
        String before,
        String after,
        String reason
) {
}
