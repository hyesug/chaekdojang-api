package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLog;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long actorId,
        String actorNickname,
        String action,
        String targetType,
        Long targetId,
        String summary,
        LocalDateTime createdAt
) {
    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActor() != null ? log.getActor().getId() : null,
                log.getActor() != null ? log.getActor().getNickname() : null,
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSummary(),
                log.getCreatedAt()
        );
    }
}
