package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignDeliveryType;
import com.chaekdojang.api.domain.dojangdan.CampaignStatus;
import com.chaekdojang.api.domain.dojangdan.ReviewCampaign;

import java.time.LocalDateTime;

public record CampaignSummaryResponse(
        Long id,
        String title,
        CampaignStatus status,
        int recruitCount,
        long applicantCount,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        String bookThumbnail,
        Long profileId,
        String profileName,
        String profileSlug,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        LocalDateTime reviewDueAt,
        int priorityInviteHours,
        LocalDateTime priorityInviteUntil,
        CampaignDeliveryType deliveryType,
        int ebookAccessExtraDays
) {
    public static CampaignSummaryResponse from(ReviewCampaign campaign, long applicantCount) {
        return new CampaignSummaryResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getStatus(),
                campaign.getRecruitCount(),
                applicantCount,
                campaign.getBook().getId(),
                campaign.getBook().getTitle(),
                campaign.getBook().getAuthor(),
                campaign.getBook().getThumbnail(),
                campaign.getProfile().getId(),
                campaign.getProfile().getDisplayName(),
                campaign.getProfile().getSlug(),
                campaign.getRecruitStartAt(),
                campaign.getRecruitEndAt(),
                campaign.getReviewDueAt(),
                campaign.getPriorityInviteHours(),
                campaign.getPriorityInviteUntil(),
                campaign.getDeliveryType(),
                campaign.getEbookAccessExtraDays()
        );
    }
}
