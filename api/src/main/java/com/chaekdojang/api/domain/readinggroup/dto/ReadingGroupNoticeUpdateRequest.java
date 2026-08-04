package com.chaekdojang.api.domain.readinggroup.dto;

import jakarta.validation.constraints.Size;

public record ReadingGroupNoticeUpdateRequest(
        @Size(max = 2000) String notice
) {
}
