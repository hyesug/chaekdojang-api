package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.ContestStatus;
import jakarta.validation.constraints.NotNull;

public record ContestStatusUpdateRequest(@NotNull ContestStatus status) {
}
