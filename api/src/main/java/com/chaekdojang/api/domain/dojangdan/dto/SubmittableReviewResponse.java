package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;

/** 서평단 제출물로 연결할 수 있는 내 독후감 후보 */
public record SubmittableReviewResponse(
        Long id,
        String excerpt,
        int length,
        int rating,
        LocalDateTime createdAt
) {
    private static final int EXCERPT_LENGTH = 120;

    public static SubmittableReviewResponse from(Review review) {
        String content = review.getContent();
        String excerpt = content.length() > EXCERPT_LENGTH
                ? content.substring(0, EXCERPT_LENGTH) + "…"
                : content;
        return new SubmittableReviewResponse(
                review.getId(),
                excerpt,
                content.length(),
                review.getRating(),
                review.getCreatedAt()
        );
    }
}
