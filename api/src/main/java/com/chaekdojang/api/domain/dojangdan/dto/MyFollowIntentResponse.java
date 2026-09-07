package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.ProfileFollowIntent;

import java.time.LocalDateTime;

public record MyFollowIntentResponse(
        Long profileId,
        String profileName,
        String profileSlug,
        LocalDateTime createdAt
) {
    public static MyFollowIntentResponse from(ProfileFollowIntent intent) {
        return new MyFollowIntentResponse(
                intent.getProfile().getId(),
                intent.getProfile().getDisplayName(),
                intent.getProfile().getSlug(),
                intent.getCreatedAt()
        );
    }
}
