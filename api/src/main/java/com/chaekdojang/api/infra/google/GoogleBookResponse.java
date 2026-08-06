package com.chaekdojang.api.infra.google;

import java.util.List;

public record GoogleBookResponse(
        List<Item> items
) {
    public record Item(VolumeInfo volumeInfo) {}

    public record VolumeInfo(
            String title,
            List<String> authors,
            String publisher,
            String description,
            List<IndustryIdentifier> industryIdentifiers,
            ImageLinks imageLinks,
            List<String> categories
    ) {}

    public record IndustryIdentifier(String type, String identifier) {}

    public record ImageLinks(String thumbnail) {}
}
