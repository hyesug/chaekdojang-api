package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroup;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupJoinPolicy;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberStatus;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupVisibility;

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
        boolean joinEnabled,
        Long ownerId,
        String ownerNickname,
        long memberCount,
        boolean member,
        boolean manager,
        ReadingGroupMemberStatus membershipStatus,
        List<ReadingGroupBookResponse> books,
        LocalDateTime createdAt
) {
    public static ReadingGroupResponse of(ReadingGroup group, long memberCount, boolean member, boolean manager, ReadingGroupMemberStatus membershipStatus, List<ReadingGroupBookResponse> books, boolean revealPrivateDetails) {
        return new ReadingGroupResponse(
                group.getId(),
                group.getName(),
                group.getSlug(),
                group.getDescription(),
                group.getImageUrl(),
                group.getVisibility(),
                group.getJoinPolicy(),
                group.isJoinEnabled(),
                revealPrivateDetails ? group.getOwner().getId() : null,
                revealPrivateDetails ? group.getOwner().getNickname() : null,
                revealPrivateDetails ? memberCount : 0,
                member,
                manager,
                membershipStatus,
                books,
                group.getCreatedAt()
        );
    }
}
