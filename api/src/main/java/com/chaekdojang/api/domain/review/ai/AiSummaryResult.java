package com.chaekdojang.api.domain.review.ai;

import java.util.List;

public record AiSummaryResult(
        String oneLineReview,
        List<String> emotionKeywords,
        String recommendedFor,
        String impressivePoint
) {
    public AiSummaryResult {
        emotionKeywords = emotionKeywords == null ? List.of() : emotionKeywords;
    }
}
