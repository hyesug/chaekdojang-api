package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.ConsentUpdateRequest;
import com.chaekdojang.api.domain.dojangdan.dto.ReviewUsageConsentResponse;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 독후감 활용 동의 관리.
 * 동의를 바꾸면 이전 동의를 revoke하고 새 행을 남긴다(append-only).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewUsageConsentService {

    private final ReviewUsageConsentRepository consentRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;

    @Value("${app.dojangdan.consent-terms-version:2026-09-01}")
    private String termsVersion;

    @Transactional
    public ReviewUsageConsent record(ReviewCampaignApplication application, boolean consentPromotional,
                                     boolean consentExcerpt, ConsentDisplayNameType displayNameType,
                                     String consentIp) {
        consentRepository.findByApplicationIdAndRevokedAtIsNull(application.getId())
                .ifPresent(ReviewUsageConsent::revoke);
        // 새 행을 넣기 전에 이전 행의 revoked_at을 먼저 반영해야 유니크 인덱스에 걸리지 않는다.
        consentRepository.flush();

        return consentRepository.save(
                ReviewUsageConsent.builder()
                        .application(application)
                        .consentPromotional(consentPromotional)
                        .consentExcerpt(consentExcerpt)
                        .displayNameType(displayNameType)
                        .termsVersion(termsVersion)
                        .consentIp(consentIp)
                        .build()
        );
    }

    @Transactional
    public ReviewUsageConsentResponse updateMyConsent(Long applicationId, ConsentUpdateRequest request,
                                                      String consentIp) {
        ReviewCampaignApplication application = requireMyApplication(applicationId);
        ReviewUsageConsent consent = record(application, request.consentPromotional(),
                request.consentExcerpt(), request.displayNameType(), consentIp);
        if (application.getReview() != null) {
            consent.linkReview(application.getReview());
        }
        return ReviewUsageConsentResponse.from(consent);
    }

    /** 동의 철회. 철회 즉시 export 대상에서 빠진다. */
    @Transactional
    public void revokeMyConsent(Long applicationId) {
        requireMyApplication(applicationId);
        ReviewUsageConsent consent = consentRepository
                .findByApplicationIdAndRevokedAtIsNull(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSENT_NOT_FOUND));
        consent.revoke();
    }

    public ReviewUsageConsentResponse getMyConsent(Long applicationId) {
        requireMyApplication(applicationId);
        return consentRepository.findByApplicationIdAndRevokedAtIsNull(applicationId)
                .map(ReviewUsageConsentResponse::from)
                .orElse(null);
    }

    public List<ReviewUsageConsentResponse> getMyConsentHistory(Long applicationId) {
        requireMyApplication(applicationId);
        return consentRepository.findByApplicationIdOrderByConsentedAtDesc(applicationId)
                .stream()
                .map(ReviewUsageConsentResponse::from)
                .toList();
    }

    /** 독후감 제출 시 유효한 동의에 독후감을 연결한다. */
    @Transactional
    public void linkReview(Long applicationId, Review review) {
        consentRepository.findByApplicationIdAndRevokedAtIsNull(applicationId)
                .ifPresent(consent -> consent.linkReview(review));
    }

    private ReviewCampaignApplication requireMyApplication(Long applicationId) {
        ReviewCampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(SecurityUtils.getCurrentUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return application;
    }
}
