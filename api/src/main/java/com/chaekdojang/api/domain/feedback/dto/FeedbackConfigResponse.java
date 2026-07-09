package com.chaekdojang.api.domain.feedback.dto;

public record FeedbackConfigResponse(
        int minChars,
        int maxChars,
        String boundaryMessage,
        String betaApplyUrl,
        boolean dailyLimitEnabled
) {
}
