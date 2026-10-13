package com.chaekdojang.api.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewCreateRequest(
        Long bookId,
        @NotBlank String content,
        @Min(1) @Max(5) int rating,
        Boolean generateAiSummary,
        Boolean hidden,
        Long previousReviewId,
        Long sourceReviewId,
        @Size(max = 10) List<@Size(max = 30) String> keywords,
        Boolean spoiler,
        /** 예약 발송 시각(KST). null 이면 바로 올린다 */
        LocalDateTime scheduledAt
) {
    public ReviewCreateRequest(Long bookId, String content, int rating, Boolean generateAiSummary, Boolean hidden,
                               Long previousReviewId, Long sourceReviewId, List<String> keywords, Boolean spoiler) {
        this(bookId, content, rating, generateAiSummary, hidden, previousReviewId, sourceReviewId, keywords, spoiler, null);
    }

    public boolean shouldGenerateAiSummary() {
        return Boolean.TRUE.equals(generateAiSummary);
    }

    public boolean shouldHide() {
        return Boolean.TRUE.equals(hidden);
    }

    public boolean hasSpoiler() {
        return Boolean.TRUE.equals(spoiler);
    }
}
