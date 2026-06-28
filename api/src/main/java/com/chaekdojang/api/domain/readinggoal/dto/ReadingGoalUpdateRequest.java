package com.chaekdojang.api.domain.readinggoal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReadingGoalUpdateRequest(
        @NotNull @Min(1) @Max(999) Integer targetCount,
        Boolean publicVisible
) {
}
