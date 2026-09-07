package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 독자용 책도장단 기능.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DojangdanService {

    private final ReviewCampaignRepository campaignRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReaderTrackRecordService trackRecordService;

    /** 공개 캠페인 목록. 작성 중(DRAFT)은 제외한다. */
    public List<CampaignSummaryResponse> getOpenCampaigns() {
        return campaignRepository.findByStatusInOrderByRecruitEndAtDesc(List.of(
                        CampaignStatus.RECRUITING,
                        CampaignStatus.CLOSED,
                        CampaignStatus.SELECTED,
                        CampaignStatus.COMPLETED))
                .stream()
                .map(campaign -> CampaignSummaryResponse.from(
                        campaign, applicationRepository.countByCampaignId(campaign.getId())))
                .toList();
    }

    public CampaignDetailResponse getCampaign(Long campaignId) {
        ReviewCampaign campaign = findPublicCampaign(campaignId);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();

        CampaignApplicationStatus myStatus = null;
        Long myApplicationId = null;
        if (userId != null) {
            var mine = applicationRepository.findByCampaignIdAndUserId(campaignId, userId);
            if (mine.isPresent()) {
                myStatus = mine.get().getStatus();
                myApplicationId = mine.get().getId();
            }
        }

        return CampaignDetailResponse.of(
                campaign,
                applicationRepository.countByCampaignId(campaignId),
                campaign.isAcceptingApplications(LocalDateTime.now()),
                myStatus,
                myApplicationId
        );
    }

    @Transactional
    public MyCampaignApplicationResponse apply(Long campaignId, CampaignApplyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewCampaign campaign = findPublicCampaign(campaignId);

        if (!campaign.isAcceptingApplications(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_RECRUITING);
        }
        if (applicationRepository.existsByCampaignIdAndUserId(campaignId, userId)) {
            throw new CustomException(ErrorCode.CAMPAIGN_ALREADY_APPLIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ReviewCampaignApplication application = applicationRepository.save(
                ReviewCampaignApplication.builder()
                        .campaign(campaign)
                        .user(user)
                        .message(request.message())
                        .build()
        );
        return MyCampaignApplicationResponse.from(application);
    }

    public List<MyCampaignApplicationResponse> getMyApplications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return applicationRepository.findByUserIdOrderByAppliedAtDesc(userId)
                .stream()
                .map(MyCampaignApplicationResponse::from)
                .toList();
    }

    public ReaderTrackRecordResponse getMyTrackRecord() {
        return trackRecordService.forUser(SecurityUtils.getCurrentUserId());
    }

    /** 이 신청에 연결할 수 있는 내 독후감 후보 (캠페인 도서로 쓴 것) */
    public List<SubmittableReviewResponse> getSubmittableReviews(Long applicationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewCampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId, application.getCampaign().getBook().getId())
                .stream()
                .map(SubmittableReviewResponse::from)
                .toList();
    }

    /** 이미 작성한 독후감을 서평단 제출물로 연결한다. */
    @Transactional
    public MyCampaignApplicationResponse submitReview(Long applicationId, CampaignReviewSubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewCampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_APPLICATION_NOT_FOUND));

        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (application.getStatus() != CampaignApplicationStatus.SELECTED) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_SELECTED);
        }

        Review review = reviewRepository.findById(request.reviewId())
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getDeletedAt() != null || !review.isAuthor(userId)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        if (review.getBook() == null
                || !review.getBook().getId().equals(application.getCampaign().getBook().getId())) {
            throw new CustomException(ErrorCode.CAMPAIGN_REVIEW_BOOK_MISMATCH);
        }

        application.submit(review);
        return MyCampaignApplicationResponse.from(application);
    }

    private ReviewCampaign findPublicCampaign(Long campaignId) {
        ReviewCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND));
        if (!campaign.isPublic()) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return campaign;
    }
}
