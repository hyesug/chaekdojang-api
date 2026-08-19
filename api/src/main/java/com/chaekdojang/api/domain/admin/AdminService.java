package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.accesslog.AccessLog;
import com.chaekdojang.api.domain.accesslog.AccessLogService;
import com.chaekdojang.api.domain.accesslog.AccessLogRepository;
import com.chaekdojang.api.domain.admin.audit.AdminAuditLogRepository;
import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.admin.dto.*;
import com.chaekdojang.api.domain.errorlog.ErrorLog;
import com.chaekdojang.api.domain.errorlog.ErrorLogRepository;
import com.chaekdojang.api.domain.errorlog.ErrorLogService;
import com.chaekdojang.api.domain.inquiry.Inquiry;
import com.chaekdojang.api.domain.inquiry.InquiryComment;
import com.chaekdojang.api.domain.inquiry.InquiryCommentRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.inquiry.dto.InquiryResponse;
import com.chaekdojang.api.domain.metrics.MetricEvent;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.UserAgentInfo;
import com.chaekdojang.api.domain.readinggroup.ReadingGroup;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupBookRepository;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberRepository;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberStatus;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupRepository;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupReviewRepository;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.domain.user.UserRole;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FILTER_MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FILTER_MAX = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final AccessLogService accessLogService;
    private final AccessLogRepository accessLogRepository;
    private final ErrorLogService errorLogService;
    private final ErrorLogRepository errorLogRepository;
    private final MetricEventRepository metricEventRepository;
    private final ReadingGroupRepository readingGroupRepository;
    private final ReadingGroupMemberRepository readingGroupMemberRepository;
    private final ReadingGroupBookRepository readingGroupBookRepository;
    private final ReadingGroupReviewRepository readingGroupReviewRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminAuditLogService adminAuditLogService;

    @Value("${app.user-activity.retention-days:90}")
    private int userActivityRetentionDays;

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
    public Page<AdminUserResponse> getUsers(
            Long adminId, String q, String ip, String deviceId,
            LocalDate joinedFrom, LocalDate joinedTo,
            LocalDate activeFrom, LocalDate activeTo,
            Boolean hasRelated, Boolean createdGroup, Pageable pageable) {
        assertAdmin(adminId);
        String normalizedQ = normalize(q);
        Long qUserId = normalizedQ.matches("\\d+") ? Long.valueOf(normalizedQ) : null;
        LocalDateTime relatedSince = LocalDateTime.now(KST)
                .minusDays(Math.max(userActivityRetentionDays, 1));
        boolean activeFilter = activeFrom != null || activeTo != null;
        return userRepository.searchForAdmin(
                        normalizedQ, qUserId, normalize(ip), normalize(deviceId),
                        joinedFrom != null ? joinedFrom.atStartOfDay() : FILTER_MIN,
                        joinedTo != null ? joinedTo.plusDays(1).atStartOfDay() : FILTER_MAX,
                        activeFrom != null ? activeFrom.atStartOfDay() : FILTER_MIN,
                        activeTo != null ? activeTo.plusDays(1).atStartOfDay() : FILTER_MAX,
                        activeFilter,
                        relatedSince, hasRelated, createdGroup, pageable)
                .map(user -> {
                    MetricEvent recent = metricEventRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
                    return new AdminUserResponse(
                            user.getId(), user.getNickname(), user.getEmail(), user.getProfileImage(), user.getRole(),
                            user.getCreatedAt(), recent != null ? recent.getCreatedAt() : null,
                            recent != null ? recent.getIp() : null,
                            recent != null ? recent.getDeviceId() : null,
                            metricEventRepository.existsRelatedSignal(user.getId(), relatedSince),
                            readingGroupRepository.countByOwnerId(user.getId())
                    );
                });
    }

    @Transactional
    public void setRole(Long adminId, Long targetUserId, UserRole role, String reason) {
        User admin = assertSuperAdmin(adminId);
        String normalizedReason = requireReason(reason);
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
                "Changed user role from " + previousRole + " to " + target.getRole() + " / reason: " + normalizedReason
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
    public void setHidden(Long adminId, Long reviewId, boolean hidden, String reason) {
        User admin = assertAdmin(adminId);
        String normalizedReason = requireReason(reason);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (hidden) review.hide(); else review.unhide();
        adminAuditLogService.record(
                admin,
                hidden ? "REVIEW_HIDDEN" : "REVIEW_UNHIDDEN",
                "REVIEW",
                review.getId(),
                "Set review hidden=" + hidden + " / reason: " + normalizedReason
        );
    }

    public List<BookReviewStatResponse> getBookStats(Long adminId) {
        assertAdmin(adminId);
        return reviewRepository.findBookReviewStats();
    }

    // ── 독서모임 관리 ───────────────────────────────────────
    public Page<AdminReadingGroupResponse> getReadingGroups(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return readingGroupRepository.findAll(pageable)
                .map(group -> AdminReadingGroupResponse.of(
                        group,
                        readingGroupMemberRepository.countByGroupIdAndStatus(group.getId(), ReadingGroupMemberStatus.APPROVED),
                        readingGroupMemberRepository.countByGroupIdAndStatus(group.getId(), ReadingGroupMemberStatus.PENDING),
                        readingGroupBookRepository.countByGroupId(group.getId())
                ));
    }

    public AdminReadingGroupDetailResponse getReadingGroupDetail(Long adminId, Long groupId) {
        assertAdmin(adminId);
        ReadingGroup group = readingGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        var members = readingGroupMemberRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId);
        var books = readingGroupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId);
        var reviews = readingGroupReviewRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId);
        Map<Long, Long> reviewCountByGroupBookId = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.getGroupBook().getId(), Collectors.counting()));
        return AdminReadingGroupDetailResponse.of(group, members, books, reviews, reviewCountByGroupBookId);
    }

    @Transactional
    public void setReadingGroupJoinEnabled(Long adminId, Long groupId, boolean enabled, String reason) {
        User admin = assertAdmin(adminId);
        String normalizedReason = requireReason(reason);
        ReadingGroup group = readingGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        group.setJoinEnabled(enabled);
        adminAuditLogService.record(
                admin,
                enabled ? "READING_GROUP_JOIN_OPENED" : "READING_GROUP_JOIN_CLOSED",
                "READING_GROUP",
                group.getId(),
                "Set joinEnabled=" + enabled + " for " + group.getName() + " / reason: " + normalizedReason
        );
    }

    @Transactional
    public void deleteReadingGroup(Long adminId, Long groupId, String reason) {
        User admin = assertAdmin(adminId);
        String normalizedReason = requireReason(reason);
        ReadingGroup group = readingGroupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        String groupName = group.getName();
        readingGroupReviewRepository.deleteAllByGroupId(groupId);
        readingGroupBookRepository.deleteAllByGroupId(groupId);
        readingGroupMemberRepository.deleteAllByGroupId(groupId);
        readingGroupRepository.delete(group);
        adminAuditLogService.record(
                admin,
                "READING_GROUP_DELETED",
                "READING_GROUP",
                groupId,
                "Deleted reading group " + groupName + " / reason: " + normalizedReason
        );
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
        Map<String, AccessLogResponse.UserMatch> userByMaskedIp = metricEventRepository
                .findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc()
                .stream()
                .filter(event -> event.getUser() != null
                        && event.getUser().getDeletedAt() == null
                        && !event.getUser().isAdmin())
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
                        pageable)
                .map(log -> AccessLogResponse.from(log, accessLogUserMatch(log, userByMaskedIp)));
    }

    public Page<MetricEventResponse> getMetricEvents(
            Long adminId,
            String q,
            String eventType,
            String userType,
            boolean excludeBackground,
            Pageable pageable) {
        assertAdmin(adminId);
        return metricEventRepository.search(
                        normalize(q),
                        normalize(eventType),
                        normalizeUserType(userType),
                        excludeBackground,
                        pageable)
                .map(MetricEventResponse::from);
    }

    public AdminDashboardSummaryResponse getDashboardSummary(Long adminId) {
        assertAdmin(adminId);
        LocalDateTime startOfToday = LocalDate.now(KST).atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        List<MetricEvent> todayMetrics = visibleMetricsSince(startOfToday);
        List<MetricEvent> todayHumanMetrics = todayMetrics.stream()
                .filter(event -> !UserAgentInfo.isBot(event.getUserAgent()))
                .toList();
        List<MetricEvent> todayBotMetrics = todayMetrics.stream()
                .filter(event -> UserAgentInfo.isBot(event.getUserAgent()))
                .toList();
        List<ErrorLog> todayErrors = visibleErrorsSince(startOfToday);
        List<AccessLog> todayAccessLogs = visibleAccessLogsSince(startOfToday);
        long todayVisitors = VisitorIdentityResolver.from(todayHumanMetrics).count(todayHumanMetrics);
        long todayBotVisitors = VisitorIdentityResolver.from(todayBotMetrics).count(todayBotMetrics);
        long todayPageViews = todayHumanMetrics.stream()
                .filter(event -> "page_view".equals(event.getEventType()))
                .count();
        long todayBookSearches = todayHumanMetrics.stream()
                .filter(event -> "book_search".equals(event.getEventType())
                        || ("page_view".equals(event.getEventType()) && "/search".equals(normalizePath(event.getPath()))))
                .count();
        long todayBookDetailViews = todayHumanMetrics.stream()
                .filter(event -> "page_view".equals(event.getEventType()))
                .filter(event -> normalizePath(event.getPath()).matches("^/books/[^/]+$"))
                .count();
        long todayReviewDetailViews = todayHumanMetrics.stream()
                .filter(event -> "page_view".equals(event.getEventType()))
                .filter(event -> normalizePath(event.getPath()).matches("^/reviews/\\d+$"))
                .count();
        long todayServerErrors = todayErrors.stream()
                .filter(error -> error.getStatus() >= 500)
                .count();
        long todaySuspiciousRequests = countSecurityOccurrences(todayErrors, todayAccessLogs);
        return new AdminDashboardSummaryResponse(
                todayVisitors,
                todayBotVisitors,
                todayPageViews,
                todayBookSearches,
                todayBookDetailViews,
                todayReviewDetailViews,
                reviewRepository.countByCreatedAtBetweenAndDeletedAtIsNull(startOfToday, startOfTomorrow),
                userRepository.countByCreatedAtBetweenAndDeletedAtIsNull(startOfToday, startOfTomorrow),
                todayServerErrors,
                todaySuspiciousRequests
        );
    }

    public List<AdminAnalyticsPageResponse> getAnalyticsPages(Long adminId) {
        assertAdmin(adminId);
        Map<String, PageAccumulator> summaries = new HashMap<>();
        List<MetricEvent> metrics = visibleHumanMetricsSince(LocalDateTime.now(KST).minusDays(30));
        VisitorIdentityResolver identities = VisitorIdentityResolver.from(metrics);
        metrics.forEach(event -> {
            String path = normalizePath(event.getPath());
            if (!"page_view".equals(event.getEventType()) && event.getDurationMs() <= 0) return;
            PageAccumulator item = summaries.computeIfAbsent(path, PageAccumulator::new);
            if ("page_view".equals(event.getEventType())) item.views++;
            item.visitors.add(identities.key(event));
            if (event.getDurationMs() > 0) {
                item.durationSumMs += event.getDurationMs();
                item.durationCount++;
            }
            item.referrers.merge(referrerLabel(event.getReferrer()), 1L, Long::sum);
            if (item.lastAt == null || event.getCreatedAt().isAfter(item.lastAt)) item.lastAt = event.getCreatedAt();
        });
        return summaries.values().stream()
                .map(PageAccumulator::toResponse)
                .sorted(Comparator.comparing(AdminAnalyticsPageResponse::lastAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminAnalyticsPageResponse::views, Comparator.reverseOrder()))
                .limit(100)
                .toList();
    }

    public List<AdminAnalyticsActionResponse> getAnalyticsActions(Long adminId) {
        assertAdmin(adminId);
        Map<String, ActionAccumulator> summaries = new HashMap<>();
        List<MetricEvent> metrics = visibleHumanMetricsSince(LocalDateTime.now(KST).minusDays(30));
        VisitorIdentityResolver identities = VisitorIdentityResolver.from(metrics);
        metrics.stream()
                .filter(event -> !"heartbeat".equals(event.getEventType()) && !"session_end".equals(event.getEventType()))
                .forEach(event -> {
                    ActionAccumulator item = summaries.computeIfAbsent(event.getEventType(), ActionAccumulator::new);
                    item.count++;
                    item.visitors.add(identities.key(event));
                    if (item.lastAt == null || event.getCreatedAt().isAfter(item.lastAt)) item.lastAt = event.getCreatedAt();
                });
        return summaries.values().stream()
                .map(ActionAccumulator::toResponse)
                .sorted(Comparator.comparing(AdminAnalyticsActionResponse::count).reversed()
                        .thenComparing(AdminAnalyticsActionResponse::lastAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(100)
                .toList();
    }

    public List<AdminSecuritySummaryResponse> getSecuritySummary(Long adminId) {
        assertAdmin(adminId);
        Map<String, SecurityAccumulator> summaries = new HashMap<>();
        visibleErrorsSince(LocalDateTime.now(KST).minusDays(30)).forEach(error -> {
            String type = suspiciousType(error.getUri(), error.getStatus());
            String ip = error.getIp() == null || error.getIp().isBlank() ? "-" : error.getIp();
            String key = "error:" + type + ":" + normalizePath(error.getUri()) + ":" + ip;
            SecurityAccumulator item = summaries.computeIfAbsent(key,
                    ignored -> new SecurityAccumulator(securitySeverity(error.getMethod(), error.getUri(), error.getStatus()), type, normalizePath(error.getUri()), ip));
            item.count++;
            if (item.lastAt == null || error.getCreatedAt().isAfter(item.lastAt)) item.lastAt = error.getCreatedAt();
        });
        visibleAccessLogsSince(LocalDateTime.now(KST).minusDays(30)).stream()
                .filter(log -> log.getStatus() >= 400 || !"기타 이상 요청".equals(suspiciousType(log.getUri(), log.getStatus())))
                .forEach(log -> {
                    String type = suspiciousType(log.getUri(), log.getStatus());
                    if ("요청 오류".equals(type) && log.getStatus() < 500) return;
                    String key = "access:" + type + ":" + normalizePath(log.getUri()) + ":" + log.getIp();
                    SecurityAccumulator item = summaries.computeIfAbsent(key,
                            ignored -> new SecurityAccumulator(securitySeverity(log.getMethod(), log.getUri(), log.getStatus()), type, normalizePath(log.getUri()), log.getIp()));
                    item.count++;
                    if (item.lastAt == null || log.getCreatedAt().isAfter(item.lastAt)) item.lastAt = log.getCreatedAt();
                });
        return summaries.values().stream()
                .map(SecurityAccumulator::toResponse)
                .sorted(Comparator.comparing(AdminSecuritySummaryResponse::lastAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminSecuritySummaryResponse::count, Comparator.reverseOrder()))
                .limit(100)
                .toList();
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

    private List<MetricEvent> visibleMetricsSince(LocalDateTime since) {
        return metricEventRepository.findVisibleSince(since);
    }

    private List<MetricEvent> visibleHumanMetricsSince(LocalDateTime since) {
        return visibleMetricsSince(since).stream()
                .filter(event -> !UserAgentInfo.isBot(event.getUserAgent()))
                .toList();
    }

    private List<AccessLog> visibleAccessLogsSince(LocalDateTime since) {
        return accessLogRepository.findVisibleSince(since);
    }

    private List<ErrorLog> visibleErrorsSince(LocalDateTime since) {
        List<Long> adminIds = userRepository.findAllByRoleInAndDeletedAtIsNull(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN))
                .stream()
                .map(User::getId)
                .toList();
        return errorLogRepository.findVisibleSince(since, adminIds.isEmpty() ? List.of(-1L) : adminIds);
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) return "/";
        String path = value.split("\\?")[0].split("#")[0];
        if (path.length() > 1 && path.endsWith("/")) return path.substring(0, path.length() - 1);
        return path.isBlank() ? "/" : path;
    }

    private String routeLabel(String value) {
        String path = normalizePath(value);
        return switch (path) {
            case "/" -> "홈 피드";
            case "/search" -> "책 검색";
            case "/library" -> "내 서재";
            case "/calendar" -> "독서 캘린더";
            case "/stats" -> "독서 통계";
            case "/write" -> "독후감 작성";
            case "/cs" -> "고객센터";
            case "/profile" -> "내 프로필";
            case "/bookmarks" -> "북마크";
            case "/notifications" -> "알림";
            case "/explore" -> "둘러보기";
            case "/auth/login" -> "로그인";
            case "/auth/register" -> "회원가입";
            case "/auth/callback" -> "소셜 로그인 처리";
            case "/setup-nickname" -> "닉네임 설정";
            case "/privacy" -> "개인정보처리방침";
            case "/terms" -> "이용약관";
            case "/dojangdan" -> "도장단";
            default -> {
                if (path.matches("^/reviews/\\d+$")) yield "독후감 상세";
                if (path.matches("^/books/[^/]+$")) yield "책 상세";
                if (path.matches("^/books/[^/]+/reviews$")) yield "책별 독후감 모음";
                if (path.matches("^/users/\\d+$") || path.matches("^/u/[^/]+$")) yield "사용자 프로필";
                if (path.startsWith("/api/")) yield "서비스 API";
                yield "기타 페이지";
            }
        };
    }

    private String metricEventLabel(String eventType) {
        return switch (eventType) {
            case "page_view" -> "페이지 조회";
            case "login_success" -> "로그인 성공";
            case "review_write_click" -> "독후감 작성 클릭";
            case "review_created" -> "독후감 등록";
            case "book_search" -> "책 검색";
            case "book_click_search" -> "검색 결과 책 클릭";
            case "share_click" -> "공유 클릭";
            case "reading_group_notice_updated" -> "독서모임 공지 수정";
            case "reading_group_book_progress_updated" -> "독서모임 책 진행 상태 수정";
            case "heartbeat" -> "체류 신호";
            case "session_end" -> "페이지 이탈";
            default -> eventType;
        };
    }

    private String referrerLabel(String referrer) {
        if (referrer == null || referrer.isBlank()) return "직접 방문";
        if (referrer.equals("chaekdojang://invite/kakao-reading-group")) return "카카오 독서모임 초대 유입";
        if (referrer.equals("chaekdojang://invite/reading-group-link")) return "독서모임 초대 링크 유입";
        String value = referrer.toLowerCase();
        if (value.contains("brunch")) return "브런치";
        if (value.contains("google")) return "구글";
        if (value.contains("naver")) return "네이버";
        if (value.contains("instagram")) return "인스타그램";
        if (value.contains("chaekdojang")) return "책도장 내부";
        return referrer;
    }

    private String topReferrer(Map<String, Long> referrers) {
        return referrers.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
    }

    private String suspiciousType(String uri, int status) {
        String path = uri == null ? "" : uri.toLowerCase();
        if (path.contains(".env")) return "환경파일 스캔";
        if (path.contains(".git")) return "Git 저장소 스캔";
        if (path.contains("php") || path.contains("wp-")) return "PHP/워드프레스 스캔";
        if (path.contains("swagger") || path.contains("api-docs")) return "API 문서 접근";
        if (path.contains("..") || path.contains("%2e")) return "경로 조작 시도";
        if (status >= 500) return "서버 오류";
        if (status >= 400) return "요청 오류";
        return "기타 이상 요청";
    }

    private AccessLogResponse.UserMatch accessLogUserMatch(
            AccessLog log, Map<String, AccessLogResponse.UserMatch> userByMaskedIp) {
        User directUser = log.getUser();
        if (directUser != null && directUser.getDeletedAt() == null && !directUser.isAdmin()) {
            return new AccessLogResponse.UserMatch(directUser.getId(), directUser.getNickname());
        }
        return userByMaskedIp.get(maskIp(log.getIp()));
    }

    private String securitySeverity(String method, String uri, int status) {
        String path = normalizePath(uri);
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/books/public/") && status == 404) return "정보";
        return status >= 500 ? "오류" : "주의";
    }

    private long countSecurityOccurrences(List<ErrorLog> errors, List<AccessLog> accessLogs) {
        long errorCount = errors.size();
        long accessCount = accessLogs.stream()
                .filter(log -> log.getStatus() >= 400 || !"기타 이상 요청".equals(suspiciousType(log.getUri(), log.getStatus())))
                .filter(log -> !"요청 오류".equals(suspiciousType(log.getUri(), log.getStatus())) || log.getStatus() >= 500)
                .count();
        return errorCount + accessCount;
    }

    private final class PageAccumulator {
        private final String path;
        private long views;
        private long durationSumMs;
        private long durationCount;
        private final Set<String> visitors = new HashSet<>();
        private final Map<String, Long> referrers = new HashMap<>();
        private LocalDateTime lastAt;

        private PageAccumulator(String path) {
            this.path = path;
        }

        private AdminAnalyticsPageResponse toResponse() {
            long avgDurationSeconds = durationCount == 0 ? 0 : Math.round((durationSumMs / 1000.0) / durationCount);
            return new AdminAnalyticsPageResponse(path, routeLabel(path), views, visitors.size(),
                    avgDurationSeconds, topReferrer(referrers), lastAt);
        }
    }

    private final class ActionAccumulator {
        private final String eventType;
        private long count;
        private final Set<String> visitors = new HashSet<>();
        private LocalDateTime lastAt;

        private ActionAccumulator(String eventType) {
            this.eventType = eventType;
        }

        private AdminAnalyticsActionResponse toResponse() {
            return new AdminAnalyticsActionResponse(eventType, metricEventLabel(eventType), count, visitors.size(), lastAt);
        }
    }

    private static final class SecurityAccumulator {
        private final String severity;
        private final String type;
        private final String uri;
        private final String ip;
        private long count;
        private LocalDateTime lastAt;

        private SecurityAccumulator(String severity, String type, String uri, String ip) {
            this.severity = severity;
            this.type = type;
            this.uri = uri;
            this.ip = ip;
        }

        private AdminSecuritySummaryResponse toResponse() {
            return new AdminSecuritySummaryResponse(severity, type, uri, count, ip, lastAt);
        }
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

    private String requireReason(String value) {
        String normalized = normalize(value);
        if (normalized.length() < 5 || normalized.length() > 500) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
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
