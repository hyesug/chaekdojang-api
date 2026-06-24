package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileApplication;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileApplicationStatus;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;

import java.time.LocalDateTime;

public record OfficialProfileApplicationResponse(
        Long id,
        Long applicantId,
        String applicantNickname,
        OfficialProfileType type,
        String displayName,
        String bio,
        String officialUrl,
        String contactEmail,
        String proofUrl,
        OfficialProfileApplicationStatus status,
        String reviewNote,
        Long profileId,
        String profileSlug,
        LocalDateTime createdAt
) {
    public static OfficialProfileApplicationResponse from(OfficialProfileApplication application) {
        return new OfficialProfileApplicationResponse(
                application.getId(),
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getType(),
                application.getDisplayName(),
                application.getBio(),
                application.getOfficialUrl(),
                application.getContactEmail(),
                application.getProofUrl(),
                application.getStatus(),
                application.getReviewNote(),
                application.getProfile() != null ? application.getProfile().getId() : null,
                application.getProfile() != null ? application.getProfile().getSlug() : null,
                application.getCreatedAt()
        );
    }
}
