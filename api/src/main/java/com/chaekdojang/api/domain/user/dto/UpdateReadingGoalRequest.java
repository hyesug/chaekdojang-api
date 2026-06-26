package com.chaekdojang.api.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateReadingGoalRequest(
        @Min(2020) @Max(2100) Integer year,
        @Min(1) @Max(999) Integer targetCount
) {
}
