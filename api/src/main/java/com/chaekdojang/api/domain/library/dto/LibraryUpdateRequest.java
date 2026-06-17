package com.chaekdojang.api.domain.library.dto;

import com.chaekdojang.api.domain.library.LibraryStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LibraryUpdateRequest(
        @NotNull LibraryStatus status,
        LocalDate completedAt
) {
}
