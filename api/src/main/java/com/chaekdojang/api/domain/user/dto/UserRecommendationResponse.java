package com.chaekdojang.api.domain.user.dto;

import com.chaekdojang.api.domain.user.User;

public record UserRecommendationResponse(
        Long id,
        String nickname,
        String profileImage,
        String bio
) {
    public static UserRecommendationResponse from(User user) {
        return new UserRecommendationResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getBio()
        );
    }
}
