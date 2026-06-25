package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;

public record ReadingGroupMyReviewResponse(
        Long id,
        String content,
        int rating,
        boolean attached,
        LocalDateTime createdAt
) {
    public static ReadingGroupMyReviewResponse of(Review review, boolean attached) {
        return new ReadingGroupMyReviewResponse(
                review.getId(),
                review.getContent(),
                review.getRating(),
                attached,
                review.getCreatedAt()
        );
    }
}
