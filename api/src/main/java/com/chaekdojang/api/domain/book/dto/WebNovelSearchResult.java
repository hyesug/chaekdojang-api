package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.BookSource;

public record WebNovelSearchResult(
        String title,
        String author,
        BookSource platform,
        String platformLabel,
        String sourceUrl,
        String externalId,
        String description,
        String thumbnail
) {
    public WebNovelSearchResult(
            String title,
            String author,
            BookSource platform,
            String platformLabel,
            String sourceUrl,
            String externalId,
            String description
    ) {
        this(title, author, platform, platformLabel, sourceUrl, externalId, description, null);
    }
}
