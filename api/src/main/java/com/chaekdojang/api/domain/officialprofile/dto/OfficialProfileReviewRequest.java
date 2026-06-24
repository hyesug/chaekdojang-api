package com.chaekdojang.api.domain.officialprofile.dto;

import jakarta.validation.constraints.Size;

public record OfficialProfileReviewRequest(
        @Size(max = 2000) String reviewNote
) {
}
