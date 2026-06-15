package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.accesslog.AccessLog;

import java.time.LocalDateTime;

public record AccessLogResponse(
        Long id,
        String ip,
        String method,
        String uri,
        int status,
        long elapsedMs,
        LocalDateTime createdAt
) {
    public static AccessLogResponse from(AccessLog log) {
        return new AccessLogResponse(
                log.getId(),
                log.getIp(),
                log.getMethod(),
                log.getUri(),
                log.getStatus(),
                log.getElapsedMs(),
                log.getCreatedAt()
        );
    }
}
