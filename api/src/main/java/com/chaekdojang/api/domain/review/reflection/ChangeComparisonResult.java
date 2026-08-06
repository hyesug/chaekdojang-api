package com.chaekdojang.api.domain.review.reflection;

public record ChangeComparisonResult(
        String previousFocus,
        String currentFocus,
        String sharedThought,
        String changedPerspective,
        String newElement,
        String reflectionQuestion,
        long inputTokens,
        long outputTokens
) {
}
