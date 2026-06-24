package com.chaekdojang.api.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminAnalyticsPageResponse(
        String path,
        String label,
        long views,
        long visitors,
        long avgDurationSeconds,
        String topReferrer,
        LocalDateTime lastAt
) {
}
