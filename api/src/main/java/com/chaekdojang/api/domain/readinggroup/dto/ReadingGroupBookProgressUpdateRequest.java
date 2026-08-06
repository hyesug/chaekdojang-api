package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupBookStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReadingGroupBookProgressUpdateRequest(
        @NotNull ReadingGroupBookStatus status,
        LocalDate deadline,
        @Size(max = 200) String note
) {
}
