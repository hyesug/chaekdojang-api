package com.chaekdojang.api.domain.review.reflection;

import java.util.List;

public record ReadingGroupAnalysisResult(
        String summary,
        List<String> commonThoughts,
        List<String> differentInterpretations,
        List<String> keyThemes,
        List<String> emotions,
        List<String> discussionQuestions,
        long inputTokens,
        long outputTokens
) {
}
