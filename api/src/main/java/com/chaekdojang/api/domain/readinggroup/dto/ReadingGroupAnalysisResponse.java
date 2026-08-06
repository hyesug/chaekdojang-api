package com.chaekdojang.api.domain.readinggroup.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingGroupAnalysisResponse(
        String summary,
        List<String> commonThoughts,
        List<String> differentInterpretations,
        List<String> keyThemes,
        List<String> emotions,
        List<String> discussionQuestions,
        int analyzedReviewCount,
        boolean stale,
        String model,
        String promptVersion,
        LocalDateTime updatedAt
) {
}
