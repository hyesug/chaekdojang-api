package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;

/** 공모전을 열 수 있는 주최 프로필 */
public record HostProfileResponse(
        Long id,
        String displayName,
        String slug,
        OfficialProfileType type,
        boolean verified,
        boolean platform
) {
    public static HostProfileResponse from(OfficialProfile profile) {
        return new HostProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getSlug(),
                profile.getType(),
                profile.isVerified(),
                profile.getType() == OfficialProfileType.PLATFORM
        );
    }
}
