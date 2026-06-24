package com.chaekdojang.api.domain.readinggroup.dto;

import jakarta.validation.constraints.NotNull;

public record ReadingGroupReviewAttachRequest(@NotNull Long reviewId) {
}
