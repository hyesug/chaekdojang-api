package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupMember;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberRole;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberStatus;

import java.time.LocalDateTime;

public record ReadingGroupMemberResponse(
        Long id,
        Long userId,
        String nickname,
        String profileImage,
        ReadingGroupMemberRole role,
        ReadingGroupMemberStatus status,
        LocalDateTime createdAt
) {
    public static ReadingGroupMemberResponse from(ReadingGroupMember member) {
        return new ReadingGroupMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImage(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}
