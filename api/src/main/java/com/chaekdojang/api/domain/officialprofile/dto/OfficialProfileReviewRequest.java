package com.chaekdojang.api.domain.officialprofile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfficialProfileReviewRequest(
        @NotBlank
        @Size(min = 5, max = 2000) String reviewNote
) {
}
