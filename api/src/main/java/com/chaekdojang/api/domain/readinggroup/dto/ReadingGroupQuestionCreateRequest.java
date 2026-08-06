package com.chaekdojang.api.domain.readinggroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingGroupQuestionCreateRequest(
        @NotBlank @Size(max = 1000) String question
) {
}
