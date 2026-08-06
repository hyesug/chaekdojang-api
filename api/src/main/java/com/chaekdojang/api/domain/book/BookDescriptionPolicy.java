package com.chaekdojang.api.domain.book;

final class BookDescriptionPolicy {

    private static final int LIKELY_EXTERNAL_EXCERPT_LENGTH = 200;
    private static final int MINIMUM_COMPLETE_SUMMARY_LENGTH = 80;
    private static final int COMPLETE_DESCRIPTION_BONUS = 10_000;
    private static final String SENTENCE_ENDINGS = ".!?。！？";

    private BookDescriptionPolicy() {
    }

    static boolean looksAbruptlyTruncated(String description) {
        if (description == null) return false;
        String normalized = description.trim();
        return normalized.length() >= LIKELY_EXTERNAL_EXCERPT_LENGTH
                && !endsWithSentenceEnding(normalized);
    }

    static String displaySynopsis(String description, boolean webNovel) {
        if (description == null || webNovel || !looksAbruptlyTruncated(description)) return description;
        int lastSentenceEnd = lastSentenceEnd(description);
        if (lastSentenceEnd + 1 >= MINIMUM_COMPLETE_SUMMARY_LENGTH) {
            return description.substring(0, lastSentenceEnd + 1).trim();
        }
        return description.trim() + "…";
    }

    static int qualityScore(String description) {
        if (description == null || description.isBlank()) return 0;
        String normalized = description.trim();
        return normalized.length()
                + (looksAbruptlyTruncated(normalized) ? 0 : COMPLETE_DESCRIPTION_BONUS);
    }

    private static boolean endsWithSentenceEnding(String value) {
        int index = value.length() - 1;
        while (index >= 0 && "\"')]}>’”」』》〉".indexOf(value.charAt(index)) >= 0) index--;
        return index >= 0 && SENTENCE_ENDINGS.indexOf(value.charAt(index)) >= 0;
    }

    private static int lastSentenceEnd(String value) {
        for (int index = value.length() - 1; index >= 0; index--) {
            if (SENTENCE_ENDINGS.indexOf(value.charAt(index)) >= 0) return index;
        }
        return -1;
    }
}
