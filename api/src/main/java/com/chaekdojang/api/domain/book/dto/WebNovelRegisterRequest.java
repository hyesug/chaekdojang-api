package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.BookSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WebNovelRegisterRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String author,
        @NotNull BookSource platform,
        @NotBlank @Size(max = 1000) String sourceUrl
) {
}
