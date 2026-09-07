package com.chaekdojang.api.domain.dojangdan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CampaignCreateRequest(
        @NotNull Long bookId,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 5000) String description,
        @Min(1) int recruitCount,
        @NotNull LocalDateTime recruitStartAt,
        @NotNull LocalDateTime recruitEndAt,
        @NotNull LocalDateTime reviewDueAt
) {
}
