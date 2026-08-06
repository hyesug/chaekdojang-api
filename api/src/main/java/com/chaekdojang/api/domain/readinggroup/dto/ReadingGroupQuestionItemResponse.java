package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupQuestion;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingGroupQuestionItemResponse(
        Long id,
        String question,
        boolean aiSuggested,
        boolean published,
        String model,
        String promptVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReadingGroupQuestionAnswerResponse> responses
) {
    public static ReadingGroupQuestionItemResponse of(
            ReadingGroupQuestion value, List<ReadingGroupQuestionAnswerResponse> responses) {
        return new ReadingGroupQuestionItemResponse(
                value.getId(), value.getQuestion(), value.isAiSuggested(), value.isPublished(),
                value.getModel(), value.getPromptVersion(), value.getCreatedAt(), value.getUpdatedAt(), responses);
    }
}
