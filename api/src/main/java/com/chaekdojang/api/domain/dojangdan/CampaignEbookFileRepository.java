package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignEbookFileRepository extends JpaRepository<CampaignEbookFile, Long> {

    Optional<CampaignEbookFile> findByCampaignId(Long campaignId);
}
