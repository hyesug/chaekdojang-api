package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.ReviewCampaign;

public record ManageCampaignDetailResponse(
        CampaignSummaryResponse campaign,
        String description,
        long appliedCount,
        long selectedCount,
        long submittedCount,
        long rejectedCount,
        long consentedReviewCount, // 홍보 활용 동의가 살아 있는 제출 독후감 수
        Integer completionRate // 완주율(%) = 제출 / 선정. 선정 전이면 null
) {
    public static ManageCampaignDetailResponse of(ReviewCampaign campaign, long appliedCount,
                                                  long selectedCount, long submittedCount,
                                                  long rejectedCount, long consentedReviewCount,
                                                  long totalApplicants) {
        long selectedTotal = selectedCount + submittedCount;
        Integer completionRate = selectedTotal == 0
                ? null
                : (int) Math.round(submittedCount * 100.0 / selectedTotal);
        return new ManageCampaignDetailResponse(
                CampaignSummaryResponse.from(campaign, totalApplicants),
                campaign.getDescription(),
                appliedCount,
                selectedCount,
                submittedCount,
                rejectedCount,
                consentedReviewCount,
                completionRate
        );
    }
}
