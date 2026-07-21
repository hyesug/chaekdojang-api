package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.BookSource;

public record WebNovelSearchResult(
        String title,
        String author,
        BookSource platform,
        String platformLabel,
        String sourceUrl,
        String externalId,
        String description
) {
}
