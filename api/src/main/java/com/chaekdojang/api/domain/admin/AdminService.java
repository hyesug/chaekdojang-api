package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.accesslog.AccessLog;
import com.chaekdojang.api.domain.accesslog.AccessLogService;
import com.chaekdojang.api.domain.admin.audit.AdminAuditLogRepository;
import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.admin.dto.*;
import com.chaekdojang.api.domain.errorlog.ErrorLogService;
import com.chaekdojang.api.domain.inquiry.Inquiry;
import com.chaekdojang.api.domain.inquiry.InquiryComment;
import com.chaekdojang.api.domain.inquiry.InquiryCommentRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.inquiry.dto.InquiryResponse;
import com.chaekdojang.api.domain.metrics.MetricEvent;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.domain.user.UserRole;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final AccessLogService accessLogService;
    private final ErrorLogService errorLogService;
    private final MetricEventRepository metricEventRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditLogService adminAuditLogService;

    // ── 권한 검증 ──────────────────────────────────────────
    private User assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.isAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    private User assertSuperAdmin(Long userId) {
        User user = assertAdmin(userId);
        if (!user.isSuperAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    // ── 회원 관리 ──────────────────────────────────────────
    public Page<AdminUserResponse> getUsers(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return userRepository.findAllByDeletedAtIsNull(pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional
    public void setRole(Long adminId, Long targetUserId, UserRole role) {
        User admin = assertSuperAdmin(adminId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (target.isSuperAdmin()) throw new CustomException(ErrorCode.FORBIDDEN); // 슈퍼 관리자는 변경 불가
        UserRole previousRole = target.getRole();
        if (role == UserRole.ADMIN) target.promoteToAdmin();
        else target.demoteToUser();
        adminAuditLogService.record(
                admin,
                "USER_ROLE_CHANGED",
                "USER",
                target.getId(),
                "Changed user role from " + previousRole + " to " + target.getRole()
        );
    }

    // ── 독후감 관리 ─────────────────────────────────────────
    public Page<AdminReviewResponse> getReviews(Long adminId, String author, String title, Pageable pageable) {
        assertAdmin(adminId);
        String a = (author != null && !author.isBlank()) ? author.trim() : null;
        String t = (title != null && !title.isBlank()) ? title.trim() : null;
        return reviewRepository.findAllByDeletedAtIsNullWithSearch(a, t, pageable)
                .map(AdminReviewResponse::from);
    }

    @Transactional
    public void setHidden(Long adminId, Long reviewId, boolean hidden) {
        User admin = assertAdmin(adminId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (hidden) review.hide(); else review.unhide();
        adminAuditLogService.record(
                admin,
                hidden ? "REVIEW_HIDDEN" : "REVIEW_UNHIDDEN",
                "REVIEW",
                review.getId(),
                "Set review hidden=" + hidden
        );
    }

    public List<BookReviewStatResponse> getBookStats(Long adminId) {
        assertAdmin(adminId);
        return reviewRepository.findBookReviewStats();
    }

    // ── 문의 관리 ──────────────────────────────────────────
    public Page<InquiryResponse> getInquiries(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return inquiryRepository.findAllByDeletedAtIsNull(pageable)
                .map(InquiryResponse::summary);
    }

    public InquiryResponse getInquiryDetail(Long adminId, Long inquiryId) {
        assertAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return InquiryResponse.from(inquiry);
    }

    // ── 접속 기록 ──────────────────────────────────────────
    public Page<AccessLogResponse> getAccessLogs(
            Long adminId,
            String q,
            String method,
            String statusGroup,
            Pageable pageable) {
        assertAdmin(adminId);
        String normalizedQ = normalize(q);
        String normalizedMethod = normalize(method);
        int[] statusRange = statusRange(statusGroup);
        List<String> excludedIps = excludedAdminIps();
        Map<String, AccessLogResponse.UserMatch> userByMaskedIp = metricEventRepository
                .findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc()
                .stream()
                .filter(event -> event.getUser() != null
                        && event.getUser().getDeletedAt() == null
                        && !event.getUser().isAdmin())
                .filter(event -> !excludedIps.contains(event.getIp()))
                .collect(Collectors.toMap(
                        event -> maskIp(event.getIp()),
                        this::toUserMatch,
                        (latest, ignored) -> latest
                ));

        return accessLogService.search(
                        normalizedQ,
                        normalizedMethod,
                        statusRange[0] > 0 ? statusRange[0] : -1,
                        statusRange[1] > 0 ? statusRange[1] : -1,
                        excludedIps,
                        pageable)
                .map(log -> AccessLogResponse.from(log, userByMaskedIp.get(log.getIp())));
    }

    public Page<MetricEventResponse> getMetricEvents(
            Long adminId,
            String q,
            String eventType,
            String userType,
            Pageable pageable) {
        assertAdmin(adminId);
        return metricEventRepository.search(normalize(q), normalize(eventType), normalizeUserType(userType), excludedAdminIps(), pageable)
                .map(MetricEventResponse::from);
    }

    public Page<ErrorLogResponse> getErrorLogs(
            Long adminId,
            String q,
            String level,
            String statusGroup,
            Pageable pageable) {
        assertAdmin(adminId);
        int[] statusRange = statusRange(statusGroup);
        return errorLogService.search(
                        normalize(q),
                        normalize(level),
                        statusRange[0] > 0 ? statusRange[0] : -1,
                        statusRange[1] > 0 ? statusRange[1] : -1,
                        pageable)
                .map(ErrorLogResponse::from);
    }

    public Page<AdminAuditLogResponse> getAuditLogs(
            Long adminId,
            String q,
            String action,
            String targetType,
            Pageable pageable) {
        assertAdmin(adminId);
        return adminAuditLogRepository.search(normalize(q), normalize(action), normalize(targetType), pageable)
                .map(AdminAuditLogResponse::from);
    }

    @Transactional
    public InquiryResponse addComment(Long adminId, Long inquiryId, String content) {
        User admin = assertAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        inquiryCommentRepository.save(InquiryComment.create(inquiry, admin, content));
        adminAuditLogService.record(
                admin,
                "INQUIRY_COMMENT_CREATED",
                "INQUIRY",
                inquiry.getId(),
                "Added admin comment to inquiry"
        );
        // 변경감지를 위해 다시 조회
        return InquiryResponse.from(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId).get());
    }

    private AccessLogResponse.UserMatch toUserMatch(MetricEvent event) {
        User user = event.getUser();
        return new AccessLogResponse.UserMatch(user.getId(), user.getNickname());
    }

    private List<String> excludedAdminIps() {
        List<String> ips = metricEventRepository.findAdminIps()
                .stream()
                .filter(ip -> ip != null && !ip.isBlank())
                .distinct()
                .toList();
        return ips.isEmpty() ? List.of("__no_admin_ip__") : ips;
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "";
        if (ip.contains(".")) {
            int lastDot = ip.lastIndexOf('.');
            return lastDot > 0 ? ip.substring(0, lastDot) + ".0" : ip;
        }
        if (ip.contains(":")) {
            int lastColon = ip.lastIndexOf(':');
            return lastColon > 0 ? ip.substring(0, lastColon) + ":0000" : ip;
        }
        return ip;
    }

    private String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : "";
    }

    private String normalizeUserType(String userType) {
        String normalized = normalize(userType);
        if ("member".equals(normalized) || "guest".equals(normalized)) return normalized;
        return "";
    }

    private int[] statusRange(String statusGroup) {
        return switch (normalize(statusGroup)) {
            case "2xx" -> new int[] {200, 300};
            case "3xx" -> new int[] {300, 400};
            case "4xx" -> new int[] {400, 500};
            case "5xx" -> new int[] {500, 600};
            default -> new int[] {0, 0};
        };
    }
}
