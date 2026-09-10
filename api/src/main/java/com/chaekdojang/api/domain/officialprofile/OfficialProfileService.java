package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.officialprofile.dto.*;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialProfileService {

    private final OfficialProfileApplicationRepository applicationRepository;
    private final OfficialProfileRepository profileRepository;
    private final OfficialProfileMemberRepository memberRepository;
    private final OfficialProfileBookRepository profileBookRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final MetricEventService metricEventService;

    @Transactional
    public OfficialProfileApplicationResponse apply(OfficialProfileApplicationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 책도장 주최 프로필은 공모전을 여는 운영진 전용이라 사용자가 신청할 수 없다.
        if (request.type() == OfficialProfileType.PLATFORM) {
            throw new CustomException(ErrorCode.OFFICIAL_PROFILE_TYPE_NOT_ALLOWED);
        }
        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        OfficialProfileApplication application = OfficialProfileApplication.builder()
                .applicant(applicant)
                .type(request.type())
                .displayName(trim(request.displayName()))
                .bio(blankToNull(request.bio()))
                .officialUrl(blankToNull(request.officialUrl()))
                .contactEmail(trim(request.contactEmail()))
                .proofUrl(blankToNull(request.proofUrl()))
                .build();
        OfficialProfileApplication saved = applicationRepository.save(application);
        metricEventService.recordCurrentRequestEvent("official_profile_applied", userId,
                "/profile-applications", Map.of(
                        "applicationId", saved.getId(),
                        "displayName", saved.getDisplayName(),
                        "profileType", saved.getType().name()
                ));
        return OfficialProfileApplicationResponse.from(saved);
    }

    public List<OfficialProfileApplicationResponse> getMyApplications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return applicationRepository.findAllByApplicantIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OfficialProfileApplicationResponse::from)
                .toList();
    }

    public OfficialProfileResponse getPublicProfile(String slug) {
        OfficialProfile profile = profileRepository.findBySlugAndStatus(slug, OfficialProfileStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return toResponse(profile);
    }

    public List<OfficialProfileResponse> getPublicProfiles() {
        return profileRepository.findAllByStatusOrderByFeaturedDescDisplayNameAsc(OfficialProfileStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<OfficialProfileApplicationResponse> getApplications(Pageable pageable) {
        return applicationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(OfficialProfileApplicationResponse::from);
    }

    public List<OfficialProfileResponse> getProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OfficialProfileApplicationResponse approveApplication(Long adminId, Long applicationId, OfficialProfileReviewRequest request) {
        User admin = assertAdmin(adminId);
        OfficialProfileApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (application.getStatus() != OfficialProfileApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        OfficialProfile profile = profileRepository.save(OfficialProfile.builder()
                .type(application.getType())
                .displayName(application.getDisplayName())
                .slug(createUniqueSlug(application.getDisplayName()))
                .bio(application.getBio())
                .officialUrl(application.getOfficialUrl())
                .contactEmail(application.getContactEmail())
                .build());
        memberRepository.save(OfficialProfileMember.owner(profile, application.getApplicant()));
        application.approve(profile, blankToNull(request.reviewNote()));
        adminAuditLogService.record(
                admin,
                "OFFICIAL_PROFILE_APPROVED",
                "OFFICIAL_PROFILE_APPLICATION",
                application.getId(),
                "Approved official profile application for " + profile.getDisplayName()
        );
        return OfficialProfileApplicationResponse.from(application);
    }

    @Transactional
    public OfficialProfileApplicationResponse rejectApplication(Long adminId, Long applicationId, OfficialProfileReviewRequest request) {
        User admin = assertAdmin(adminId);
        OfficialProfileApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (application.getStatus() != OfficialProfileApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        application.reject(blankToNull(request.reviewNote()));
        adminAuditLogService.record(
                admin,
                "OFFICIAL_PROFILE_REJECTED",
                "OFFICIAL_PROFILE_APPLICATION",
                application.getId(),
                "Rejected official profile application for " + application.getDisplayName()
        );
        return OfficialProfileApplicationResponse.from(application);
    }

    /**
     * 관리자가 공식 프로필을 신청 절차 없이 직접 만든다.
     * 책도장이 직접 주최하는 공모전(PLATFORM)이나 도서관 프로필을 운영진이 바로 열 수 있게 하기 위한 통로다.
     * 만든 관리자는 그 프로필의 운영자로 함께 등록된다.
     */
    @Transactional
    public OfficialProfileResponse createProfile(Long adminId, OfficialProfileCreateRequest request) {
        User admin = assertAdmin(adminId);
        OfficialProfile profile = profileRepository.save(OfficialProfile.builder()
                .type(request.type())
                .displayName(trim(request.displayName()))
                .slug(createUniqueSlug(request.displayName()))
                .bio(blankToNull(request.bio()))
                .officialUrl(blankToNull(request.officialUrl()))
                .contactEmail(blankToNull(request.contactEmail()))
                .build());
        memberRepository.save(OfficialProfileMember.owner(profile, admin));
        adminAuditLogService.record(
                admin,
                "OFFICIAL_PROFILE_CREATED",
                "OFFICIAL_PROFILE",
                profile.getId(),
                "Created official profile " + profile.getDisplayName() + " (" + profile.getType() + ")"
        );
        return toResponse(profile);
    }

    @Transactional
    public OfficialProfileResponse updateProfile(Long adminId, Long profileId, OfficialProfileUpdateRequest request) {
        User admin = assertAdmin(adminId);
        OfficialProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        profile.update(
                trim(request.displayName()),
                blankToNull(request.bio()),
                blankToNull(request.imageUrl()),
                blankToNull(request.officialUrl()),
                blankToNull(request.instagramUrl()),
                blankToNull(request.brunchUrl()),
                blankToNull(request.tumblbugUrl()),
                blankToNull(request.contactEmail()),
                request.status(),
                request.verified(),
                request.featured()
        );
        adminAuditLogService.record(
                admin,
                "OFFICIAL_PROFILE_UPDATED",
                "OFFICIAL_PROFILE",
                profile.getId(),
                "Updated official profile " + profile.getDisplayName()
        );
        return toResponse(profile);
    }

    @Transactional
    public OfficialProfileResponse addBook(Long adminId, Long profileId, OfficialProfileBookAddRequest request) {
        User admin = assertAdmin(adminId);
        OfficialProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        if (!profileBookRepository.existsByProfileIdAndBookId(profileId, book.getId())) {
            profileBookRepository.save(OfficialProfileBook.of(profile, book));
            adminAuditLogService.record(
                    admin,
                    "OFFICIAL_PROFILE_BOOK_ADDED",
                    "OFFICIAL_PROFILE",
                    profile.getId(),
                    "Added book " + book.getTitle() + " to " + profile.getDisplayName()
            );
        }
        return toResponse(profile);
    }

    @Transactional
    public OfficialProfileResponse removeBook(Long adminId, Long profileId, Long bookId) {
        User admin = assertAdmin(adminId);
        OfficialProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        profileBookRepository.deleteByProfileIdAndBookId(profileId, bookId);
        adminAuditLogService.record(
                admin,
                "OFFICIAL_PROFILE_BOOK_REMOVED",
                "OFFICIAL_PROFILE",
                profile.getId(),
                "Removed book " + bookId + " from " + profile.getDisplayName()
        );
        return toResponse(profile);
    }

    private OfficialProfileResponse toResponse(OfficialProfile profile) {
        List<OfficialProfileBookResponse> books = profileBookRepository.findAllByProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(profileBook -> OfficialProfileBookResponse.of(
                        profileBook,
                        reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(profileBook.getBook().getId())))
                .toList();
        return OfficialProfileResponse.of(profile, books);
    }

    private User assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.isAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    private String createUniqueSlug(String displayName) {
        String base = Normalizer.normalize(displayName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "profile";
        if (base.length() > 80) base = base.substring(0, 80).replaceAll("-+$", "");
        String slug = base;
        int suffix = 2;
        while (profileRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
