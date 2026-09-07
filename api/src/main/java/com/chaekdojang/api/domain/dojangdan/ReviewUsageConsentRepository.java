package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewUsageConsentRepository extends JpaRepository<ReviewUsageConsent, Long> {

    Optional<ReviewUsageConsent> findByApplicationIdAndRevokedAtIsNull(Long applicationId);

    List<ReviewUsageConsent> findByApplicationIdInAndRevokedAtIsNull(Collection<Long> applicationIds);

    List<ReviewUsageConsent> findByApplicationIdOrderByConsentedAtDesc(Long applicationId);

    /** 제출까지 끝났고 홍보 활용 동의가 살아 있는 독후감 수 = 지금 내보낼 수 있는 건수 */
    @Query("""
            SELECT COUNT(c) FROM ReviewUsageConsent c
            WHERE c.application.campaign.id = :campaignId
              AND c.revokedAt IS NULL
              AND c.consentPromotional = true
              AND c.application.status = com.chaekdojang.api.domain.dojangdan.CampaignApplicationStatus.SUBMITTED
              AND c.application.review.deletedAt IS NULL
            """)
    long countExportableConsents(@Param("campaignId") Long campaignId);
}
