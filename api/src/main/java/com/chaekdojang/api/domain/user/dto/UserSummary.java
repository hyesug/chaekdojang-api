package com.chaekdojang.api.domain.user.dto;

import com.chaekdojang.api.domain.user.User;

public record UserSummary(
        Long id,
        String nickname,
        String profileImage
) {
    public static UserSummary from(User user) {
        if (user.getDeletedAt() != null) {
            return new UserSummary(null, "탈퇴한 사용자", null);
        }
        return new UserSummary(user.getId(), user.getNickname(), user.getProfileImage());
    }
}
