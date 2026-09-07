package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewCampaignRepository extends JpaRepository<ReviewCampaign, Long> {

    List<ReviewCampaign> findByStatusInOrderByRecruitEndAtDesc(Collection<CampaignStatus> statuses);

    List<ReviewCampaign> findByProfileIdInOrderByCreatedAtDesc(Collection<Long> profileIds);
}
