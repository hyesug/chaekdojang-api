package com.chaekdojang.api.domain.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackCommentRequest(
        @NotBlank String content,
        @NotBlank @Size(max = 80) String sessionId
) {
}
