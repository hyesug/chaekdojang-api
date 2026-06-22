package com.chaekdojang.api.domain.review.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewVisibilityRequest(
        @NotNull Boolean hidden
) {
}
