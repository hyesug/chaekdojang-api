package com.chaekdojang.api.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewUpdateRequest(
        Long bookId,
        @NotBlank String content,
        @Min(1) @Max(5) int rating,
        Boolean generateAiSummary,
        Boolean hidden
) {
    public boolean shouldGenerateAiSummary() {
        return Boolean.TRUE.equals(generateAiSummary);
    }

    public boolean shouldHide() {
        return Boolean.TRUE.equals(hidden);
    }
}
