package com.chaekdojang.api.domain.feedback.dto;

public record FeedbackCommentResponse(
        FeedbackResult feedback,
        int minChars,
        int maxChars,
        String boundaryMessage,
        String betaApplyUrl
) {
    public static FeedbackCommentResponse of(
            FeedbackResult feedback,
            int minChars,
            int maxChars,
            String boundaryMessage,
            String betaApplyUrl
    ) {
        return new FeedbackCommentResponse(
                feedback,
                minChars,
                maxChars,
                boundaryMessage,
                betaApplyUrl == null || betaApplyUrl.isBlank() ? null : betaApplyUrl
        );
    }
}
