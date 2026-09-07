package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;

public record ManagedProfileResponse(
        Long id,
        String displayName,
        String slug,
        OfficialProfileType type,
        boolean verified
) {
    public static ManagedProfileResponse from(OfficialProfile profile) {
        return new ManagedProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getSlug(),
                profile.getType(),
                profile.isVerified()
        );
    }
}
