package com.chaekdojang.api.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminSecuritySummaryResponse(
        String severity,
        String type,
        String uri,
        long count,
        String ip,
        LocalDateTime lastAt
) {
}
