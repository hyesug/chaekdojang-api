package com.chaekdojang.api.domain.officialprofile.dto;

import jakarta.validation.constraints.NotNull;

public record OfficialProfileBookAddRequest(
        @NotNull Long bookId
) {
}
