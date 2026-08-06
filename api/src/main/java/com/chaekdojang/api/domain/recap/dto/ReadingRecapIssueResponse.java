package com.chaekdojang.api.domain.recap.dto;

import com.chaekdojang.api.domain.recap.ReadingRecapIssue;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReadingRecapIssueResponse(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        String title,
        String summary,
        int ownReviewCount,
        int followingReviewCount,
        int savedReviewCount,
        int continuedReviewCount,
        Long featuredReviewId,
        Long memoryReviewId,
        String memoryBookTitle,
        boolean read,
        LocalDateTime createdAt
) {
    public static ReadingRecapIssueResponse from(ReadingRecapIssue value) {
        return new ReadingRecapIssueResponse(
                value.getId(), value.getPeriodStart(), value.getPeriodEnd(), value.getTitle(), value.getSummary(),
                value.getOwnReviewCount(), value.getFollowingReviewCount(), value.getSavedReviewCount(),
                value.getContinuedReviewCount(),
                value.getFeaturedReview() != null ? value.getFeaturedReview().getId() : null,
                value.getMemoryReview() != null ? value.getMemoryReview().getId() : null,
                value.getMemoryBookTitle(), value.getReadAt() != null, value.getCreatedAt());
    }
}
