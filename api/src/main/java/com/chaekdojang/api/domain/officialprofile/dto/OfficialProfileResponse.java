package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileStatus;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;

import java.util.List;

public record OfficialProfileResponse(
        Long id,
        OfficialProfileType type,
        String displayName,
        String slug,
        String bio,
        String imageUrl,
        String officialUrl,
        String instagramUrl,
        String brunchUrl,
        String tumblbugUrl,
        String contactEmail,
        OfficialProfileStatus status,
        boolean verified,
        boolean featured,
        List<OfficialProfileBookResponse> books
) {
    public static OfficialProfileResponse of(OfficialProfile profile, List<OfficialProfileBookResponse> books) {
        return new OfficialProfileResponse(
                profile.getId(),
                profile.getType(),
                profile.getDisplayName(),
                profile.getSlug(),
                profile.getBio(),
                profile.getImageUrl(),
                profile.getOfficialUrl(),
                profile.getInstagramUrl(),
                profile.getBrunchUrl(),
                profile.getTumblbugUrl(),
                profile.getContactEmail(),
                profile.getStatus(),
                profile.isVerified(),
                profile.isFeatured(),
                books
        );
    }
}
