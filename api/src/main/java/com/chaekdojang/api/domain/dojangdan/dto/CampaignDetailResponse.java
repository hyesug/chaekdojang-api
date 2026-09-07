package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignApplicationStatus;
import com.chaekdojang.api.domain.dojangdan.ReviewCampaign;

public record CampaignDetailResponse(
        CampaignSummaryResponse campaign,
        String description,
        boolean acceptingApplications,
        /** 지금이 관심 독자 우선 신청 기간인지 */
        boolean priorityWindow,
        /** 우선 초대 대상이거나 공개 모집이 시작돼서 지금 신청할 수 있는지 */
        boolean canApplyNow,
        CampaignApplicationStatus myApplicationStatus,
        Long myApplicationId
) {
    public static CampaignDetailResponse of(ReviewCampaign campaign, long applicantCount,
                                            boolean acceptingApplications, boolean priorityWindow,
                                            boolean canApplyNow,
                                            CampaignApplicationStatus myStatus, Long myApplicationId) {
        return new CampaignDetailResponse(
                CampaignSummaryResponse.from(campaign, applicantCount),
                campaign.getDescription(),
                acceptingApplications,
                priorityWindow,
                canApplyNow,
                myStatus,
                myApplicationId
        );
    }
}
