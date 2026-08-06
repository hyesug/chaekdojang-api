package com.chaekdojang.api.domain.readinggroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingGroupQuestionAnswerRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
