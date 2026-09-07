package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.ConsentDisplayNameType;
import com.chaekdojang.api.domain.dojangdan.ReviewUsageConsent;

import java.time.LocalDateTime;

public record ReviewUsageConsentResponse(
        Long id,
        Long applicationId,
        boolean consentPromotional,
        boolean consentExcerpt,
        ConsentDisplayNameType displayNameType,
        String termsVersion,
        LocalDateTime consentedAt,
        LocalDateTime revokedAt
) {
    public static ReviewUsageConsentResponse from(ReviewUsageConsent consent) {
        return new ReviewUsageConsentResponse(
                consent.getId(),
                consent.getApplication().getId(),
                consent.isConsentPromotional(),
                consent.isConsentExcerpt(),
                consent.getDisplayNameType(),
                consent.getTermsVersion(),
                consent.getConsentedAt(),
                consent.getRevokedAt()
        );
    }
}
