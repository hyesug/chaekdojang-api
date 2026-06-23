package com.chaekdojang.api.domain.book;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

final class BookSlugGenerator {

    private static final Map<String, String> KNOWN_SLUGS = Map.ofEntries(
            Map.entry("데미안", "demian"),
            Map.entry("인간실격", "human-disqualification"),
            Map.entry("인간 실격", "human-disqualification"),
            Map.entry("이방인", "the-stranger"),
            Map.entry("싯다르타", "siddhartha"),
            Map.entry("스토너", "stoner"),
            Map.entry("불편한 편의점", "inconvenient-convenience-store"),
            Map.entry("모순", "mosun"),
            Map.entry("긴긴밤", "the-long-long-night"),
            Map.entry("소년이 온다", "human-acts"),
            Map.entry("물고기는 존재하지 않는다", "why-fish-dont-exist")
    );

    private BookSlugGenerator() {
    }

    static String create(String title, String author, String isbn13, Long id) {
        String normalizedTitle = normalizeTitle(title);
        String known = KNOWN_SLUGS.get(normalizedTitle);
        if (known != null) return known;

        String ascii = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (ascii.isBlank() && author != null) {
            ascii = Normalizer.normalize(author, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
        }
        if (ascii.isBlank()) {
            ascii = isbn13 != null && !isbn13.isBlank() ? "book-" + isbn13 : "book-" + id;
        }
        return ascii.length() > 150 ? ascii.substring(0, 150).replaceAll("-+$", "") : ascii;
    }

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        return title
                .replaceAll("\\([^)]*\\)|\\[[^]]*\\]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
