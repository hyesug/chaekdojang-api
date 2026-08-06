package com.chaekdojang.api.domain.book;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BookWorkKey {
    private static final String EDITION_KEYWORDS = "초판|개정|양장|표지|오리지널|리커버|특별|한정|무선|반양장|클래식|전집";
    private static final Pattern NOTE_PATTERN = Pattern.compile("\\(([^)]*)\\)|\\[([^]]*)\\]");
    private static final Pattern EDITION_COLON_PATTERN = Pattern.compile("[:：]\\s*(?=.*(" + EDITION_KEYWORDS + ")).*$");
    private static final Pattern EDITION_SUFFIX_PATTERN = Pattern.compile("\\s+(더클래식\\s*)?세계문학.*$");

    private BookWorkKey() {
    }

    static String of(Book book) {
        if (book == null) return "";
        return normalizeTitle(book.getTitle()) + "\u0000" + normalizeAuthor(book.getAuthor());
    }

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        String withoutNotes = stripEditionNotes(title);
        return EDITION_SUFFIX_PATTERN.matcher(EDITION_COLON_PATTERN.matcher(withoutNotes).replaceAll(" "))
                .replaceAll(" ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^가-힣a-z0-9]", "");
    }

    private static String stripEditionNotes(String title) {
        Matcher matcher = NOTE_PATTERN.matcher(title);
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            String note = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String replacement = isVolumeNote(note) ? matcher.group() : " ";
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static boolean isVolumeNote(String note) {
        String value = note == null ? "" : note.replaceAll("\\s+", "");
        return value.matches("^(상|중|하)$") || value.matches("^(제)?\\d{1,2}(권|부|편|집|권째)?$");
    }

    private static String normalizeAuthor(String author) {
        if (author == null) return "";
        return author.split("[,;/·]")[0]
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^가-힣a-z0-9]", "");
    }
}
