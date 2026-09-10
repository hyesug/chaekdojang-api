package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;

/** 공모전 응모작으로 연결할 수 있는 내 독후감 후보 */
public record ContestSubmittableReviewResponse(
        Long reviewId,
        Long bookId,
        String bookTitle,
        String excerpt,
        int length,
        int rating,
        LocalDateTime createdAt
) {
    private static final int EXCERPT_LENGTH = 120;

    public static ContestSubmittableReviewResponse from(Review review) {
        String content = review.getContent();
        String excerpt = content.length() > EXCERPT_LENGTH
                ? content.substring(0, EXCERPT_LENGTH) + "…"
                : content;
        return new ContestSubmittableReviewResponse(
                review.getId(),
                review.getBook() == null ? null : review.getBook().getId(),
                review.getBook() == null ? null : review.getBook().getTitle(),
                excerpt,
                content.length(),
                review.getRating(),
                review.getCreatedAt()
        );
    }
}
