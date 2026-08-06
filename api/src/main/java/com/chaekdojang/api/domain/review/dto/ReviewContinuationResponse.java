package com.chaekdojang.api.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewContinuationResponse(
        SourceReview sourceReview,
        boolean sourceUnavailable,
        List<ContinuationItem> continuations,
        long totalCount,
        boolean canContinue
) {
    public record SourceReview(
            Long id,
            Long authorId,
            String authorNickname,
            LocalDateTime createdAt
    ) {
    }

    public record ContinuationItem(
            Long id,
            Long authorId,
            String authorNickname,
            String excerpt,
            LocalDateTime createdAt
    ) {
    }
}
