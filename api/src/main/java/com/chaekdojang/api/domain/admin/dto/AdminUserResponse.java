package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRole;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String nickname,
        String email,
        String profileImage,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime recentActivityAt,
        String recentIp,
        String recentDeviceId,
        boolean relatedAccountExists,
        long createdGroupCount
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getNickname(), u.getEmail(),
                u.getProfileImage(), u.getRole(), u.getCreatedAt(), null, null, null, false, 0);
    }
}
