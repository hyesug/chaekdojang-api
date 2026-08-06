package com.chaekdojang.api.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
        Boolean spoiler
) {
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
