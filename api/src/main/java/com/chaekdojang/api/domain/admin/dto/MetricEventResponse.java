package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.metrics.MetricEvent;

import java.time.LocalDateTime;

public record MetricEventResponse(
        Long id,
        Long userId,
        String nickname,
        String eventType,
        String sessionId,
        String path,
        String referrer,
        long durationMs,
        String device,
        String ip,
        LocalDateTime createdAt
) {
    public static MetricEventResponse from(MetricEvent event) {
        return new MetricEventResponse(
                event.getId(),
                event.getUser() != null ? event.getUser().getId() : null,
                event.getUser() != null ? event.getUser().getNickname() : null,
                event.getEventType(),
                event.getSessionId(),
                event.getPath(),
                event.getReferrer(),
                event.getDurationMs(),
                event.getDevice(),
                event.getIp(),
                event.getCreatedAt()
        );
    }
}
