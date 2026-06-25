package com.chaekdojang.api.domain.readinggroup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReadingGroupBookAddRequest(
        @NotNull Long bookId,
        @Size(max = 200) String note
) {
}
