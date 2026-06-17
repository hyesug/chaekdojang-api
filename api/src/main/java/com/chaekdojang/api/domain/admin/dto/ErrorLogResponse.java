package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.errorlog.ErrorLog;

import java.time.LocalDateTime;

public record ErrorLogResponse(
        Long id,
        String level,
        String method,
        String uri,
        int status,
        String exceptionType,
        String message,
        String ip,
        Long userId,
        LocalDateTime createdAt
) {
    public static ErrorLogResponse from(ErrorLog log) {
        return new ErrorLogResponse(
                log.getId(),
                log.getLevel(),
                log.getMethod(),
                log.getUri(),
                log.getStatus(),
                log.getExceptionType(),
                log.getMessage(),
                log.getIp(),
                log.getUserId(),
                log.getCreatedAt()
        );
    }
}
