package com.chaekdojang.api.domain.library.dto;

import com.chaekdojang.api.domain.library.LibraryStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LibraryAddRequest(
        @NotNull Long bookId,
        @NotNull LibraryStatus status,
        LocalDate completedAt
) {
}
