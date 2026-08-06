package com.chaekdojang.api.domain.review.reflection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpQuestionUpdateRequest(
        @NotBlank @Size(max = 600) String question
) {
}
