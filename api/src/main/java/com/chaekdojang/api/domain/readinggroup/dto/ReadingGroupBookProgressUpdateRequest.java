package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupBookStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReadingGroupBookProgressUpdateRequest(
        @NotNull ReadingGroupBookStatus status,
        LocalDate deadline
) {
}
