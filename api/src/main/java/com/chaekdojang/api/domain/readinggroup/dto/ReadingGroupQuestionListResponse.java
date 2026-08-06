package com.chaekdojang.api.domain.readinggroup.dto;

import java.util.List;

public record ReadingGroupQuestionListResponse(
        boolean canManage,
        boolean canRespond,
        boolean canAddQuestions,
        List<ReadingGroupQuestionItemResponse> questions
) {
}
