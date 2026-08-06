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

    static boolean sameWork(Book first, Book second) {
        if (first == null || second == null) return false;
        String firstTitle = normalizeTitle(first.getTitle());
        String secondTitle = normalizeTitle(second.getTitle());
        if (firstTitle.isBlank() || !firstTitle.equals(secondTitle)) return false;

        String firstAuthor = normalizeAuthor(first.getAuthor());
        String secondAuthor = normalizeAuthor(second.getAuthor());
        if (firstAuthor.isBlank() || secondAuthor.isBlank()) return firstAuthor.equals(secondAuthor);
        if (firstAuthor.equals(secondAuthor)) return true;
        if (Math.min(firstAuthor.length(), secondAuthor.length()) < 5) return false;

        int maximumDistance = (int) Math.ceil(Math.max(firstAuthor.length(), secondAuthor.length()) * 0.4);
        return editDistance(firstAuthor, secondAuthor) <= maximumDistance;
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

    private static int editDistance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        for (int index = 0; index <= second.length(); index++) previous[index] = index;
        for (int firstIndex = 1; firstIndex <= first.length(); firstIndex++) {
            int[] current = new int[second.length() + 1];
            current[0] = firstIndex;
            for (int secondIndex = 1; secondIndex <= second.length(); secondIndex++) {
                int substitutionCost = first.charAt(firstIndex - 1) == second.charAt(secondIndex - 1) ? 0 : 1;
                current[secondIndex] = Math.min(
                        Math.min(current[secondIndex - 1] + 1, previous[secondIndex] + 1),
                        previous[secondIndex - 1] + substitutionCost);
            }
            previous = current;
        }
        return previous[second.length()];
    }
}
