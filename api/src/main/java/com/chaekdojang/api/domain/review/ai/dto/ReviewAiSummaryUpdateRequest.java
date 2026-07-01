package com.chaekdojang.api.domain.review.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewAiSummaryUpdateRequest(
        @NotBlank @Size(max = 60) String oneLineReview,
        @NotEmpty @Size(min = 3, max = 5) List<@NotBlank @Size(max = 50) String> emotionKeywords,
        @NotBlank @Size(max = 120) String recommendedFor,
        @NotBlank @Size(max = 100) String impressivePoint
) {
}
