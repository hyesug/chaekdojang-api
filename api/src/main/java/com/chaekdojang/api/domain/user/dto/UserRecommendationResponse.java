package com.chaekdojang.api.domain.user.dto;

import com.chaekdojang.api.domain.user.User;

import java.util.ArrayList;
import java.util.List;

public record UserRecommendationResponse(
        Long id,
        String nickname,
        String profileImage,
        String bio,
        int overlapCount,
        List<String> reasons
) {
    public static UserRecommendationResponse from(User user) {
        return from(user, 0, 0, false);
    }

    public static UserRecommendationResponse from(
            User user, int overlapCount, int ratingSimilarityScore, boolean lifeBookMatched) {
        List<String> reasons = new ArrayList<>();
        if (overlapCount > 0) reasons.add("완독한 책 " + overlapCount + "권이 같습니다.");
        if (ratingSimilarityScore > 0) reasons.add("같은 책에 남긴 별점 흐름이 비슷합니다.");
        if (lifeBookMatched) reasons.add("같은 인생책을 선택했습니다.");
        return new UserRecommendationResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getBio(),
                overlapCount,
                List.copyOf(reasons)
        );
    }
}
