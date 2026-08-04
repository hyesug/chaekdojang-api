package com.chaekdojang.api.domain.metrics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record MetricEventRequest(
        @NotBlank @Size(max = 50) String eventType,
        @NotBlank @Size(max = 80) String sessionId,
        @NotBlank @Size(max = 500) String path,
        @Size(max = 500) String referrer,
        long durationMs,
        @Size(max = 80) String device,
        @Size(max = 80) String deviceId,
        Map<String, Object> meta
) {
}
