package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.*;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingGroupResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        ReadingGroupVisibility visibility,
        ReadingGroupJoinPolicy joinPolicy,
        Long ownerId,
        String ownerNickname,
        boolean member,
        boolean manager,
        List<ReadingGroupBookResponse> books,
        LocalDateTime createdAt
) {
    public static ReadingGroupResponse of(ReadingGroup group, boolean member, boolean manager, List<ReadingGroupBookResponse> books) {
        return new ReadingGroupResponse(
                group.getId(),
                group.getName(),
                group.getSlug(),
                group.getDescription(),
                group.getImageUrl(),
                group.getVisibility(),
                group.getJoinPolicy(),
                group.getOwner().getId(),
                group.getOwner().getNickname(),
                member,
                manager,
                books,
                group.getCreatedAt()
        );
    }
}
