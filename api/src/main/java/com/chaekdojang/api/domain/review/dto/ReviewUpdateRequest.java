package com.chaekdojang.api.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewUpdateRequest(
        Long bookId,
        @NotBlank String content,
        @Min(1) @Max(5) int rating,
        Boolean generateAiSummary,
        Boolean hidden,
        @Size(max = 10) List<@Size(max = 30) String> keywords,
        Boolean spoiler,
        /** 예약 시각(KST). 예약 독후감을 고칠 때 이 값을 보내야 예약이 유지된다. null 이면 hidden 값대로 바로 반영 */
        LocalDateTime scheduledAt
) {
    public ReviewUpdateRequest(Long bookId, String content, int rating, Boolean generateAiSummary, Boolean hidden,
                               List<String> keywords, Boolean spoiler) {
        this(bookId, content, rating, generateAiSummary, hidden, keywords, spoiler, null);
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
