package com.chaekdojang.api.domain.dojangdan.dto;

import java.time.LocalDateTime;

/**
 * 출판사 화면에 보여줄 제출 독후감 1건.
 * 활용에 동의하지 않은 독후감은 content를 비우고 링크와 통계만 넘긴다.
 */
public record CampaignReviewSummaryResponse(
        Long applicationId,
        Long reviewId,
        String displayName,
        boolean consentPromotional,
        boolean consentExcerpt,
        int reviewLength,
        LocalDateTime submittedAt,
        String reviewUrl,
        String content
) {
}
