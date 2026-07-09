package com.chaekdojang.api.domain.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FeedbackResult(
        @JsonProperty("not_review") boolean notReview,
        String message,
        @JsonProperty("core_theme") String coreTheme,
        List<String> strengths,
        List<String> improvements,
        @JsonProperty("sentence_examples") List<FeedbackSentenceExample> sentenceExamples,
        @JsonProperty("title_suggestions") List<String> titleSuggestions,
        @JsonProperty("deep_question") String deepQuestion
) {
}
