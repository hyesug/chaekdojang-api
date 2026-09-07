package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileMemberRepository;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 출판사·작가용 책도장단 운영 기능.
 * 권한은 전역 역할이 아니라 official_profile_members(프로필 소속)로 판단한다.
 * 한 사람이 여러 출판사를 관리할 수 있고, 전역 역할을 늘리지 않아도 되기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DojangdanManageService {

    private static final Map<CampaignStatus, Set<CampaignStatus>> ALLOWED_TRANSITIONS = Map.of(
            CampaignStatus.DRAFT, Set.of(CampaignStatus.RECRUITING),
            CampaignStatus.RECRUITING, Set.of(CampaignStatus.DRAFT, CampaignStatus.CLOSED),
            CampaignStatus.CLOSED, Set.of(CampaignStatus.RECRUITING, CampaignStatus.SELECTED),
            CampaignStatus.SELECTED, Set.of(CampaignStatus.COMPLETED),
            CampaignStatus.COMPLETED, Set.of()
    );

    private final ReviewCampaignRepository campaignRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;
    private final ReviewUsageConsentRepository consentRepository;
    private final OfficialProfileRepository profileRepository;
    private final OfficialProfileMemberRepository profileMemberRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ReaderTrackRecordService trackRecordService;
    private final CampaignInviteService inviteService;
    private final CampaignEbookService ebookService;
    private final CampaignAccessGuard accessGuard;

    /** 내가 운영자로 등록된 공식 프로필 목록 */
    public List<ManagedProfileResponse> getManagedProfiles() {
        Long userId = SecurityUtils.getCurrentUserId();
        return profileMemberRepository.findByUserId(userId).stream()
                .map(member -> ManagedProfileResponse.from(member.getProfile()))
                .toList();
    }

    @Transactional
    public ManageCampaignDetailResponse createCampaign(Long profileId, CampaignCreateRequest request) {
        requireProfileAccess(profileId);
        validatePeriod(request.recruitStartAt(), request.recruitEndAt(), request.reviewDueAt());

        OfficialProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));

        ReviewCampaign campaign = campaignRepository.save(
                ReviewCampaign.builder()
                        .profile(profile)
                        .book(book)
                        .title(request.title())
                        .description(request.description())
                        .recruitCount(request.recruitCount())
                        .recruitStartAt(request.recruitStartAt())
                        .recruitEndAt(request.recruitEndAt())
                        .reviewDueAt(request.reviewDueAt())
                        .priorityInviteHours(request.priorityInviteHours())
                        .deliveryType(request.deliveryType())
                        .ebookAccessExtraDays(request.ebookAccessExtraDays())
                        .build()
        );
        return detailOf(campaign);
    }

    public List<CampaignSummaryResponse> getMyCampaigns() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> profileIds = profileMemberRepository.findByUserId(userId).stream()
                .map(member -> member.getProfile().getId())
                .toList();
        if (profileIds.isEmpty()) return List.of();

        return campaignRepository.findByProfileIdInOrderByCreatedAtDesc(profileIds).stream()
                .map(campaign -> CampaignSummaryResponse.from(
                        campaign, applicationRepository.countByCampaignId(campaign.getId())))
                .toList();
    }

    public ManageCampaignDetailResponse getCampaignDetail(Long campaignId) {
        return detailOf(requireCampaignAccess(campaignId));
    }

    @Transactional
    public ManageCampaignDetailResponse updateCampaign(Long campaignId, CampaignUpdateRequest request) {
        ReviewCampaign campaign = requireCampaignAccess(campaignId);
        validatePeriod(request.recruitStartAt(), request.recruitEndAt(), request.reviewDueAt());
        campaign.update(request.title(), request.description(), request.recruitCount(),
                request.recruitStartAt(), request.recruitEndAt(), request.reviewDueAt(),
                request.priorityInviteHours(), request.deliveryType(), request.ebookAccessExtraDays());
        return detailOf(campaign);
    }

    @Transactional
    public ManageCampaignDetailResponse updateStatus(Long campaignId, CampaignStatusUpdateRequest request) {
        ReviewCampaign campaign = requireCampaignAccess(campaignId);
        if (!ALLOWED_TRANSITIONS.getOrDefault(campaign.getStatus(), Set.of()).contains(request.status())) {
            throw new CustomException(ErrorCode.CAMPAIGN_STATUS_TRANSITION_NOT_ALLOWED);
        }

        if (request.status() == CampaignStatus.RECRUITING) {
            boolean firstOpen = campaign.getPriorityInviteUntil() == null;
            campaign.startRecruiting();
            // 우선 초대는 캠페인을 처음 열 때 한 번만 보낸다.
            if (firstOpen && campaign.getPriorityInviteHours() > 0) {
                User actor = userRepository.findById(SecurityUtils.getCurrentUserId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                inviteService.sendPriorityInvites(campaign, actor);
            }
        } else {
            campaign.changeStatus(request.status());
        }
        return detailOf(campaign);
    }

    public List<CampaignApplicantResponse> getApplicants(Long campaignId) {
        requireCampaignAccess(campaignId);
        List<ReviewCampaignApplication> applications =
                applicationRepository.findByCampaignIdOrderByAppliedAtAsc(campaignId);

        Map<Long, ReaderTrackRecordResponse> trackRecords = trackRecordService.forUsers(
                applications.stream().map(application -> application.getUser().getId()).toList());
        Map<Long, EbookAccessGrant> grants = ebookService
                .getGrants(applications.stream().map(ReviewCampaignApplication::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        grant -> grant.getApplication().getId(), grant -> grant));

        return applications.stream()
                .map(application -> CampaignApplicantResponse.of(
                        application,
                        trackRecords.getOrDefault(application.getUser().getId(),
                                ReaderTrackRecordResponse.empty()),
                        grants.get(application.getId())))
                .toList();
    }

    /** 선정 처리. rejectOthers가 true면 나머지 신청자를 모두 미선정으로 확정한다. */
    @Transactional
    public List<CampaignApplicantResponse> select(Long campaignId, CampaignSelectRequest request) {
        ReviewCampaign campaign = requireCampaignAccess(campaignId);
        if (campaign.getStatus() != CampaignStatus.CLOSED && campaign.getStatus() != CampaignStatus.SELECTED) {
            throw new CustomException(ErrorCode.CAMPAIGN_STATUS_TRANSITION_NOT_ALLOWED);
        }

        User actor = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Set<Long> selectedIds = Set.copyOf(request.applicationIds());

        for (ReviewCampaignApplication application :
                applicationRepository.findByCampaignIdOrderByAppliedAtAsc(campaignId)) {
            if (selectedIds.contains(application.getId())) {
                if (application.getStatus() == CampaignApplicationStatus.APPLIED) {
                    application.select();
                    // 전자책 캠페인이면 선정과 동시에 열람 권한이 생긴다.
                    ebookService.grantAccess(application);
                    notificationService.send(application.getUser(), actor,
                            NotificationType.CAMPAIGN_SELECTED, campaignId);
                }
            } else if (request.rejectOthers()
                    && application.getStatus() == CampaignApplicationStatus.APPLIED) {
                application.reject();
                notificationService.send(application.getUser(), actor,
                        NotificationType.CAMPAIGN_REJECTED, campaignId);
            }
        }

        if (campaign.getStatus() == CampaignStatus.CLOSED) {
            campaign.changeStatus(CampaignStatus.SELECTED);
        }
        return getApplicants(campaignId);
    }

    private ManageCampaignDetailResponse detailOf(ReviewCampaign campaign) {
        Long campaignId = campaign.getId();
        return ManageCampaignDetailResponse.of(
                campaign,
                applicationRepository.countByCampaignIdAndStatus(campaignId, CampaignApplicationStatus.APPLIED),
                applicationRepository.countByCampaignIdAndStatus(campaignId, CampaignApplicationStatus.SELECTED),
                applicationRepository.countByCampaignIdAndStatus(campaignId, CampaignApplicationStatus.SUBMITTED),
                applicationRepository.countByCampaignIdAndStatus(campaignId, CampaignApplicationStatus.REJECTED),
                consentRepository.countExportableConsents(campaignId),
                applicationRepository.countByCampaignId(campaignId)
        );
    }

    private void validatePeriod(java.time.LocalDateTime start, java.time.LocalDateTime end,
                                java.time.LocalDateTime due) {
        if (!start.isBefore(end) || due.isBefore(end)) {
            throw new CustomException(ErrorCode.CAMPAIGN_INVALID_PERIOD);
        }
    }

    private ReviewCampaign requireCampaignAccess(Long campaignId) {
        return accessGuard.requireCampaignAccess(campaignId);
    }

    private void requireProfileAccess(Long profileId) {
        accessGuard.requireProfileAccess(profileId);
    }
}
