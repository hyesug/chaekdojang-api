package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignApplicationStatus;
import com.chaekdojang.api.domain.dojangdan.ReviewCampaign;

public record CampaignDetailResponse(
        CampaignSummaryResponse campaign,
        String description,
        boolean acceptingApplications,
        CampaignApplicationStatus myApplicationStatus,
        Long myApplicationId
) {
    public static CampaignDetailResponse of(ReviewCampaign campaign, long applicantCount,
                                            boolean acceptingApplications,
                                            CampaignApplicationStatus myStatus, Long myApplicationId) {
        return new CampaignDetailResponse(
                CampaignSummaryResponse.from(campaign, applicantCount),
                campaign.getDescription(),
                acceptingApplications,
                myStatus,
                myApplicationId
        );
    }
}
