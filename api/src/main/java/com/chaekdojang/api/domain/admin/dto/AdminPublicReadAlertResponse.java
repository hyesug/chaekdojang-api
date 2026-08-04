package com.chaekdojang.api.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminPublicReadAlertResponse(
        String actorKey,
        Long userId,
        String nickname,
        String ip,
        long requestCount,
        int windowMinutes,
        List<String> categories,
        LocalDateTime lastAt,
        String message
) {
}
