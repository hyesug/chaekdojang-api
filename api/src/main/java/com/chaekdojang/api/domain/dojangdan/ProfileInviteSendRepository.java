package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ProfileInviteSendRepository extends JpaRepository<ProfileInviteSend, Long> {

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);

    long countByProfileIdAndUserIdAndSentAtAfter(Long profileId, Long userId, LocalDateTime since);

    long countByCampaignId(Long campaignId);
}
