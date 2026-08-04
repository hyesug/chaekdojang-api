package com.chaekdojang.api.domain.user.dto;

public record PublicUserProfileResponse(
        Long id,
        String nickname,
        String bio,
        String profileImage,
        long reviewCount,
        long followerCount,
        long followingCount,
        UserProfileResponse.LibrarySummary librarySummary,
        UserProfileResponse.ReadingGoalSummary readingGoal,
        UserProfileResponse.LifeBook lifeBook
) {
    public static PublicUserProfileResponse from(UserProfileResponse profile) {
        return new PublicUserProfileResponse(
                profile.id(),
                profile.nickname(),
                profile.bio(),
                profile.profileImage(),
                profile.reviewCount(),
                profile.followerCount(),
                profile.followingCount(),
                profile.librarySummary(),
                profile.readingGoal(),
                profile.lifeBook()
        );
    }
}
