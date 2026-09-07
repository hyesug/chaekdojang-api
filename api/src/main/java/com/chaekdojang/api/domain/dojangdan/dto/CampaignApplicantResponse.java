package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignApplicationStatus;
import com.chaekdojang.api.domain.dojangdan.EbookAccessGrant;
import com.chaekdojang.api.domain.dojangdan.ReviewCampaignApplication;

import java.time.LocalDateTime;

/**
 * 출판사 화면에 보여줄 신청자 정보.
 * 이메일·연락처는 절대 담지 않는다. 개인정보 제3자 제공이 되기 때문에
 * 출판사에는 닉네임과 서평단 이력만 노출하고, 연락은 플랫폼이 대행한다.
 */
public record CampaignApplicantResponse(
        Long applicationId,
        Long userId,
        String nickname,
        String profileImage,
        String message,
        CampaignApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime selectedAt,
        LocalDateTime submittedAt,
        Long reviewId,
        Integer reviewLength,
        ReaderTrackRecordResponse trackRecord,
        // 전자책 캠페인일 때만 채워진다. 유출 추적과 참여 확인의 근거다.
        Integer ebookOpenCount,
        LocalDateTime ebookFirstOpenedAt,
        LocalDateTime ebookExpiresAt
) {
    public static CampaignApplicantResponse of(ReviewCampaignApplication application,
                                               ReaderTrackRecordResponse trackRecord,
                                               EbookAccessGrant grant) {
        var review = application.getReview();
        return new CampaignApplicantResponse(
                application.getId(),
                application.getUser().getId(),
                application.getUser().getNickname(),
                application.getUser().getProfileImage(),
                application.getMessage(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getSelectedAt(),
                application.getSubmittedAt(),
                review == null ? null : review.getId(),
                review == null ? null : review.getContent().length(),
                trackRecord,
                grant == null ? null : grant.getOpenCount(),
                grant == null ? null : grant.getFirstOpenedAt(),
                grant == null ? null : grant.getExpiresAt()
        );
    }
}
