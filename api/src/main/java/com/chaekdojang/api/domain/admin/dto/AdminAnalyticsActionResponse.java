package com.chaekdojang.api.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminAnalyticsActionResponse(
        String eventType,
        String label,
        long count,
        long visitors,
        LocalDateTime lastAt
) {
}
