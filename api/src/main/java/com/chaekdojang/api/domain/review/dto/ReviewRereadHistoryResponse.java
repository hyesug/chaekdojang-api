package com.chaekdojang.api.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewRereadHistoryResponse(
        List<HistoryItem> records,
        Long latestReviewId,
        boolean canCreateReread
) {
    public record HistoryItem(
            Long id,
            int sequence,
            int rating,
            boolean hidden,
            boolean current,
            LocalDateTime createdAt
    ) {
    }
}
