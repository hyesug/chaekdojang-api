package com.chaekdojang.api.domain.feedback.dto;

public record FeedbackImprovement(
        String point,
        String before,
        String direction,
        String after,
        String reason
) {
}
