package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewCampaignApplicationRepository extends JpaRepository<ReviewCampaignApplication, Long> {

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);

    Optional<ReviewCampaignApplication> findByCampaignIdAndUserId(Long campaignId, Long userId);

    List<ReviewCampaignApplication> findByCampaignIdOrderByAppliedAtAsc(Long campaignId);

    List<ReviewCampaignApplication> findByCampaignIdAndStatusIn(Long campaignId, Collection<CampaignApplicationStatus> statuses);

    List<ReviewCampaignApplication> findByUserIdOrderByAppliedAtDesc(Long userId);

    List<ReviewCampaignApplication> findByUserIdIn(Collection<Long> userIds);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, CampaignApplicationStatus status);

    Optional<ReviewCampaignApplication> findByReviewId(Long reviewId);
}
