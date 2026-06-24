package com.chaekdojang.api.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatReportRequest(
        @NotBlank
        @Size(min = 5, max = 500)
        String reason
) {
}
