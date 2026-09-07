package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignApplicationStatus;
import com.chaekdojang.api.domain.dojangdan.ReviewCampaignApplication;

import java.time.LocalDateTime;

public record MyCampaignApplicationResponse(
        Long id,
        Long campaignId,
        String campaignTitle,
        Long bookId,
        String bookTitle,
        String bookThumbnail,
        String profileName,
        CampaignApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime selectedAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewDueAt,
        Long reviewId
) {
    public static MyCampaignApplicationResponse from(ReviewCampaignApplication application) {
        var campaign = application.getCampaign();
        return new MyCampaignApplicationResponse(
                application.getId(),
                campaign.getId(),
                campaign.getTitle(),
                campaign.getBook().getId(),
                campaign.getBook().getTitle(),
                campaign.getBook().getThumbnail(),
                campaign.getProfile().getDisplayName(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getSelectedAt(),
                application.getSubmittedAt(),
                campaign.getReviewDueAt(),
                application.getReview() == null ? null : application.getReview().getId()
        );
    }
}
