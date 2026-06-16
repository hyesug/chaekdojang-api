package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.accesslog.AccessLog;

import java.time.LocalDateTime;

public record AccessLogResponse(
        Long id,
        String ip,
        Long matchedUserId,
        String matchedNickname,
        String method,
        String uri,
        int status,
        long elapsedMs,
        LocalDateTime createdAt
) {
    public static AccessLogResponse from(AccessLog log) {
        return from(log, null);
    }

    public static AccessLogResponse from(AccessLog log, UserMatch userMatch) {
        return new AccessLogResponse(
                log.getId(),
                log.getIp(),
                userMatch != null ? userMatch.userId() : null,
                userMatch != null ? userMatch.nickname() : null,
                log.getMethod(),
                log.getUri(),
                log.getStatus(),
                log.getElapsedMs(),
                log.getCreatedAt()
        );
    }

    public record UserMatch(Long userId, String nickname) {
    }
}
