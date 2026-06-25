package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroup;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupJoinPolicy;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupVisibility;

import java.time.LocalDateTime;

public record AdminReadingGroupResponse(
        Long id,
        String name,
        String slug,
        String description,
        ReadingGroupVisibility visibility,
        ReadingGroupJoinPolicy joinPolicy,
        boolean joinEnabled,
        Long ownerId,
        String ownerNickname,
        long memberCount,
        long pendingCount,
        long bookCount,
        LocalDateTime createdAt
) {
    public static AdminReadingGroupResponse of(
            ReadingGroup group,
            long memberCount,
            long pendingCount,
            long bookCount
    ) {
        return new AdminReadingGroupResponse(
                group.getId(),
                group.getName(),
                group.getSlug(),
                group.getDescription(),
                group.getVisibility(),
                group.getJoinPolicy(),
                group.isJoinEnabled(),
                group.getOwner().getId(),
                group.getOwner().getNickname(),
                memberCount,
                pendingCount,
                bookCount,
                group.getCreatedAt()
        );
    }
}
