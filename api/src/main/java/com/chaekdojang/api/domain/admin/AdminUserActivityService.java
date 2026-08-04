package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.accesslog.AccessLog;
import com.chaekdojang.api.domain.accesslog.AccessLogRepository;
import com.chaekdojang.api.domain.admin.dto.AdminPublicReadAlertResponse;
import com.chaekdojang.api.domain.admin.dto.AdminUserActivityResponse;
import com.chaekdojang.api.domain.metrics.MetricEvent;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberRepository;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupRepository;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserAuthProviderRepository;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserActivityService {

    public static final String NOTICE = "이 정보는 동일 계정 사용 가능성을 검토하기 위한 참고 자료입니다. "
            + "공용 네트워크나 동일 가구 사용자는 같은 IP로 접속할 수 있으므로 자동 제재의 근거로 사용하지 마세요.";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FILTER_MAX = LocalDateTime.of(3000, 1, 1, 0, 0);
    private static final Set<String> SAFE_META_KEYS = Set.of(
            "groupId", "groupSlug", "groupName", "bookId", "bookTitle", "reviewId",
            "memberUserId", "memberNickname", "applicationId", "displayName", "profileType",
            "oauthProvider", "userId", "mode"
    );

    private final UserRepository userRepository;
    private final UserAuthProviderRepository authProviderRepository;
    private final MetricEventRepository metricEventRepository;
    private final AccessLogRepository accessLogRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingGroupRepository readingGroupRepository;
    private final ReadingGroupMemberRepository readingGroupMemberRepository;

    @Value("${app.user-activity.retention-days:90}")
    private int retentionDays;

    @Value("${app.user-activity.public-read-alert-window-minutes:5}")
    private int publicReadAlertWindowMinutes;

    @Value("${app.user-activity.public-read-alert-threshold:120}")
    private int publicReadAlertThreshold;

    public AdminUserActivityResponse getUserActivity(
            Long adminId,
            Long targetUserId,
            String eventType,
            LocalDate from,
            LocalDate to,
            boolean includeTechnical,
            Pageable pageable
    ) {
        assertAdmin(adminId);
        User target = userRepository.findById(targetUserId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime since = LocalDateTime.now(KST).minusDays(Math.max(retentionDays, 1));
        List<MetricEvent> targetEvents = metricEventRepository
                .findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(targetUserId, since);

        Page<AdminUserActivityResponse.TimelineEvent> timeline = metricEventRepository.findUserTimeline(
                        targetUserId,
                        normalize(eventType),
                        from != null ? from.atStartOfDay() : since,
                        to != null ? to.plusDays(1).atStartOfDay() : FILTER_MAX,
                        includeTechnical,
                        pageable)
                .map(this::toTimelineEvent);

        MetricEvent firstLogin = targetEvents.stream()
                .filter(this::isLoginEvent)
                .min(Comparator.comparing(MetricEvent::getCreatedAt))
                .orElse(null);
        MetricEvent recentLogin = targetEvents.stream()
                .filter(this::isLoginEvent)
                .max(Comparator.comparing(MetricEvent::getCreatedAt))
                .orElse(null);
        MetricEvent recentActivity = targetEvents.stream().findFirst().orElse(null);
        List<String> deviceIds = targetEvents.stream()
                .map(MetricEvent::getDeviceId)
                .filter(this::hasText)
                .distinct()
                .toList();

        var providers = authProviderRepository.findAllByUserIdOrderByCreatedAtAsc(targetUserId)
                .stream()
                .map(provider -> provider.getProvider())
                .distinct()
                .toList();

        return new AdminUserActivityResponse(
                new AdminUserActivityResponse.BasicInfo(
                        target.getId(), target.getNickname(), target.getCreatedAt(), providers,
                        reviewRepository.countByAuthorIdAndDeletedAtIsNullAndHiddenFalse(targetUserId),
                        readingGroupRepository.countByOwnerId(targetUserId),
                        readingGroupMemberRepository.countApprovedMembershipsByUserId(targetUserId)
                ),
                new AdminUserActivityResponse.AccessInfo(
                        firstLogin != null ? firstLogin.getCreatedAt() : target.getCreatedAt(),
                        recentLogin != null ? recentLogin.getCreatedAt() : null,
                        recentActivity != null ? recentActivity.getCreatedAt() : null,
                        recentActivity != null ? recentActivity.getIp() : null,
                        recentActivity != null ? recentActivity.getDevice() : null,
                        recentActivity != null ? recentActivity.getBrowser() : null,
                        recentActivity != null ? recentActivity.getOperatingSystem() : null,
                        deviceIds
                ),
                timeline,
                findRelatedAccounts(target, targetEvents, since),
                NOTICE
        );
    }

    public List<AdminPublicReadAlertResponse> getPublicReadAlerts(Long adminId) {
        assertAdmin(adminId);
        int window = Math.max(publicReadAlertWindowMinutes, 1);
        int threshold = Math.max(publicReadAlertThreshold, 1);
        List<AccessLog> logs = accessLogRepository.findRecentPublicReads(LocalDateTime.now(KST).minusMinutes(window));
        Map<String, List<AccessLog>> groups = new LinkedHashMap<>();
        logs.forEach(log -> {
            groups.computeIfAbsent("ip:" + log.getIp(), ignored -> new ArrayList<>()).add(log);
            if (log.getUser() != null) {
                groups.computeIfAbsent("user:" + log.getUser().getId(), ignored -> new ArrayList<>()).add(log);
            }
        });
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= threshold)
                .map(entry -> toPublicReadAlert(entry.getKey(), entry.getValue(), window))
                .sorted(Comparator.comparingLong(AdminPublicReadAlertResponse::requestCount).reversed())
                .toList();
    }

    private List<AdminUserActivityResponse.RelatedAccount> findRelatedAccounts(
            User target, List<MetricEvent> targetEvents, LocalDateTime since) {
        List<String> deviceIds = targetEvents.stream().map(MetricEvent::getDeviceId)
                .filter(this::hasText).distinct().toList();
        List<String> ips = targetEvents.stream().map(MetricEvent::getIp)
                .filter(this::hasText).distinct().toList();
        Map<Long, User> candidates = new LinkedHashMap<>();
        if (!deviceIds.isEmpty()) {
            metricEventRepository.findRelatedByDeviceIds(target.getId(), deviceIds, since)
                    .forEach(event -> candidates.put(event.getUser().getId(), event.getUser()));
        }
        if (!ips.isEmpty()) {
            metricEventRepository.findRelatedByIps(target.getId(), ips, since)
                    .forEach(event -> candidates.put(event.getUser().getId(), event.getUser()));
        }
        if (targetEvents.stream().anyMatch(this::isGroupRelationEvent)) {
            Set<String> targetGroupIds = targetEvents.stream()
                    .filter(this::isGroupRelationEvent)
                    .map(event -> event.getMeta() != null ? event.getMeta().get("groupId") : null)
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            metricEventRepository.findRelatedGroupActivity(target.getId(), since)
                    .stream()
                    .filter(event -> event.getMeta() != null
                            && targetGroupIds.contains(String.valueOf(event.getMeta().get("groupId"))))
                    .forEach(event -> candidates.put(event.getUser().getId(), event.getUser()));
        }

        return candidates.values().stream()
                .map(candidate -> scoreRelatedAccount(
                        targetEvents,
                        metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                                candidate.getId(), since),
                        candidate))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(AdminUserActivityResponse.RelatedAccount::score).reversed()
                        .thenComparing(AdminUserActivityResponse.RelatedAccount::lastRelatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AdminUserActivityResponse.RelatedAccount scoreRelatedAccount(
            List<MetricEvent> targetEvents, List<MetricEvent> candidateEvents, User candidate) {
        Set<String> targetDevices = values(targetEvents, MetricEvent::getDeviceId);
        Set<String> candidateDevices = values(candidateEvents, MetricEvent::getDeviceId);
        Set<String> sharedDevices = intersection(targetDevices, candidateDevices);
        Set<String> sharedIps = intersection(values(targetEvents, MetricEvent::getIp), values(candidateEvents, MetricEvent::getIp));

        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (!sharedDevices.isEmpty()) {
            score += 60;
            reasons.add("동일 기기 ID " + sharedDevices.size() + "개");
        }
        boolean sameIpAndUserAgent = hasNearMatch(targetEvents, candidateEvents, true, Duration.ofHours(24));
        if (sameIpAndUserAgent) {
            score += 25;
            reasons.add("동일 IP 및 브라우저에서 가까운 시간에 활동");
        } else if (!sharedIps.isEmpty()) {
            score += 5;
            reasons.add("동일 IP 사용");
        }
        if (registeredFromSameIpWithin(targetEvents, candidateEvents, Duration.ofHours(24))) {
            score += 10;
            reasons.add("24시간 이내 같은 IP에서 가입");
        }
        GroupSequenceSignal groupSignal = findGroupSequence(targetEvents, candidateEvents, Duration.ofMinutes(30));
        if (groupSignal != null) {
            score += 10;
            String groupName = stringMeta(groupSignal.created().getMeta(), "groupName",
                    stringMeta(groupSignal.created().getMeta(), "groupSlug", "같은 독서모임"));
            long minutes = Duration.between(groupSignal.created().getCreatedAt(), groupSignal.joined().getCreatedAt()).toMinutes();
            reasons.add("「" + groupName + "」 모임 생성 "
                    + (minutes < 1 ? "1분 이내" : minutes + "분 후") + " 다른 계정 가입");
        }
        int alternating = alternatingActivityCount(targetEvents, candidateEvents, Duration.ofMinutes(10));
        if (alternating >= 3) reasons.add("같은 IP에서 활동 흐름이 짧은 간격으로 " + alternating + "회 이어짐");

        boolean high = !sharedDevices.isEmpty() || score >= 70;
        String strength = high ? "높음" : score >= 40 ? "검토 필요" : "낮음";
        LocalDateTime lastAt = java.util.stream.Stream.concat(targetEvents.stream(), candidateEvents.stream())
                .filter(event -> sharedIps.contains(event.getIp()) || sharedDevices.contains(event.getDeviceId()))
                .map(MetricEvent::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (groupSignal != null && (lastAt == null || groupSignal.joined().getCreatedAt().isAfter(lastAt))) {
            lastAt = groupSignal.joined().getCreatedAt();
        }
        return new AdminUserActivityResponse.RelatedAccount(
                candidate.getId(), candidate.getNickname(), score, strength, List.copyOf(reasons), lastAt);
    }

    private boolean hasNearMatch(List<MetricEvent> left, List<MetricEvent> right, boolean requireUserAgent, Duration maxGap) {
        for (MetricEvent a : left) {
            if (!hasText(a.getIp())) continue;
            for (MetricEvent b : right) {
                if (!a.getIp().equals(b.getIp())) continue;
                if (requireUserAgent && (!hasText(a.getUserAgent()) || !a.getUserAgent().equals(b.getUserAgent()))) continue;
                if (within(a.getCreatedAt(), b.getCreatedAt(), maxGap)) return true;
            }
        }
        return false;
    }

    private boolean registeredFromSameIpWithin(List<MetricEvent> left, List<MetricEvent> right, Duration maxGap) {
        return left.stream().filter(event -> "user_registered".equals(event.getEventType()))
                .anyMatch(a -> right.stream()
                        .filter(event -> "user_registered".equals(event.getEventType()))
                        .anyMatch(b -> hasText(a.getIp()) && a.getIp().equals(b.getIp())
                                && within(a.getCreatedAt(), b.getCreatedAt(), maxGap)));
    }

    private GroupSequenceSignal findGroupSequence(List<MetricEvent> left, List<MetricEvent> right, Duration maxGap) {
        GroupSequenceSignal signal = findOneWayGroupSequence(left, right, maxGap);
        return signal != null ? signal : findOneWayGroupSequence(right, left, maxGap);
    }

    private GroupSequenceSignal findOneWayGroupSequence(
            List<MetricEvent> creators, List<MetricEvent> joiners, Duration maxGap) {
        for (MetricEvent created : creators) {
            if (!"reading_group_created".equals(created.getEventType())) continue;
            for (MetricEvent joined : joiners) {
                if (!joined.getEventType().equals("reading_group_joined")
                        && !joined.getEventType().equals("reading_group_join_requested")) continue;
                if (sameMetaId(created, joined, "groupId")
                        && !joined.getCreatedAt().isBefore(created.getCreatedAt())
                        && within(created.getCreatedAt(), joined.getCreatedAt(), maxGap)) {
                    return new GroupSequenceSignal(created, joined);
                }
            }
        }
        return null;
    }

    private int alternatingActivityCount(List<MetricEvent> left, List<MetricEvent> right, Duration maxGap) {
        record OwnedEvent(int owner, MetricEvent event) {}
        List<OwnedEvent> combined = new ArrayList<>();
        left.forEach(event -> combined.add(new OwnedEvent(0, event)));
        right.forEach(event -> combined.add(new OwnedEvent(1, event)));
        combined.sort(Comparator.comparing(item -> item.event().getCreatedAt()));
        int count = 0;
        for (int i = 1; i < combined.size(); i++) {
            OwnedEvent previous = combined.get(i - 1);
            OwnedEvent current = combined.get(i);
            if (previous.owner() != current.owner()
                    && hasText(previous.event().getIp())
                    && previous.event().getIp().equals(current.event().getIp())
                    && within(previous.event().getCreatedAt(), current.event().getCreatedAt(), maxGap)) count++;
        }
        return count;
    }

    private AdminUserActivityResponse.TimelineEvent toTimelineEvent(MetricEvent event) {
        Map<String, Object> meta = safeMeta(event.getMeta());
        return new AdminUserActivityResponse.TimelineEvent(
                event.getId(), event.getEventType(), eventLabel(event.getEventType()),
                eventDescription(event, meta), event.getPath(), event.getCreatedAt(), event.getIp(),
                event.getDeviceId(), event.getDevice(), event.getBrowser(), event.getOperatingSystem(), meta);
    }

    private AdminPublicReadAlertResponse toPublicReadAlert(String actorKey, List<AccessLog> logs, int window) {
        AccessLog latest = logs.stream().max(Comparator.comparing(AccessLog::getCreatedAt)).orElseThrow();
        User user = actorKey.startsWith("user:") ? latest.getUser() : null;
        List<String> categories = logs.stream().map(log -> publicCategory(log.getUri())).distinct().toList();
        return new AdminPublicReadAlertResponse(
                actorKey,
                user != null ? user.getId() : null,
                user != null ? user.getNickname() : null,
                latest.getIp(),
                logs.size(),
                window,
                categories,
                latest.getCreatedAt(),
                "일반 이용을 차단하지 않는 참고 경고입니다. 자동 제재 근거로 사용하지 마세요."
        );
    }

    private String publicCategory(String uri) {
        if (uri.startsWith("/api/books/")) return "공개 책";
        if (uri.startsWith("/api/reviews/")) return "공개 독후감";
        return "공개 프로필";
    }

    private String eventLabel(String type) {
        return switch (type) {
            case "user_registered" -> "회원가입";
            case "login_succeeded", "login_success" -> "로그인 성공";
            case "reading_group_created" -> "독서모임 생성";
            case "reading_group_joined" -> "독서모임 가입";
            case "reading_group_join_requested" -> "독서모임 가입 요청";
            case "reading_group_member_approved" -> "독서모임 가입 승인";
            case "reading_group_book_added" -> "독서모임 책 추가";
            case "reading_group_review_attached" -> "독서모임 독후감 연결";
            case "review_created" -> "독후감 작성";
            case "profile_updated" -> "프로필 수정";
            case "official_profile_applied" -> "공식 프로필 신청";
            case "page_view" -> "페이지 조회";
            case "heartbeat" -> "체류 신호";
            case "session_end" -> "세션 종료";
            default -> type;
        };
    }

    private String eventDescription(MetricEvent event, Map<String, Object> meta) {
        String type = event.getEventType();
        String group = stringMeta(meta, "groupName", stringMeta(meta, "groupSlug", ""));
        String book = stringMeta(meta, "bookTitle", "");
        return switch (type) {
            case "reading_group_created" -> "독서모임 생성: " + group;
            case "reading_group_joined" -> "독서모임 가입: " + group;
            case "reading_group_join_requested" -> "독서모임 가입 요청: " + group;
            case "reading_group_member_approved" -> "가입 승인: " + stringMeta(meta, "memberNickname", "회원");
            case "reading_group_book_added" -> "독서모임에 책 추가: " + book;
            case "reading_group_review_attached" -> "모임 책에 독후감 연결: " + book;
            case "review_created" -> book.isBlank() ? "독후감 작성" : "독후감 작성: " + book;
            case "official_profile_applied" -> "공식 프로필 신청: " + stringMeta(meta, "displayName", "");
            case "page_view" -> pageViewDescription(event.getPath());
            default -> eventLabel(type);
        };
    }

    private String pageViewDescription(String value) {
        String path = value == null ? "/" : value.split("[?#]", 2)[0];
        if (path.isBlank() || "/".equals(path)) return "홈 피드 조회";
        if (path.startsWith("/search")) return "책 검색 화면 조회";
        if (path.matches("^/books/[^/]+/reviews/?$")) return "책별 독후감 조회";
        if (path.matches("^/books/[^/]+/?$")) return "책 상세 조회";
        if (path.matches("^/reviews/[^/]+/?$")) return "독후감 상세 조회";
        if (path.matches("^/groups/[^/]+/books/[^/]+/result/?$")) return "독서모임 AI 결과 조회";
        if (path.startsWith("/groups")) return "독서모임 조회";
        if (path.startsWith("/library")) return "서재 조회";
        if (path.startsWith("/profile") || path.startsWith("/users/")
                || path.startsWith("/u/") || path.startsWith("/profiles/")) return "프로필 조회";
        if (path.startsWith("/write")) return "독후감 작성 화면 조회";
        if (path.startsWith("/notifications")) return "알림 조회";
        return "페이지 조회";
    }

    private Map<String, Object> safeMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>();
        SAFE_META_KEYS.forEach(key -> {
            Object value = meta.get(key);
            if (value instanceof Number || value instanceof Boolean) safe.put(key, value);
            else if (value instanceof String text) safe.put(key, text.substring(0, Math.min(text.length(), 200)));
        });
        return Map.copyOf(safe);
    }

    private Set<String> values(List<MetricEvent> events, Function<MetricEvent, String> mapper) {
        return events.stream().map(mapper).filter(this::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.retainAll(right);
        return values;
    }

    private boolean within(LocalDateTime a, LocalDateTime b, Duration maxGap) {
        return Duration.between(a, b).abs().compareTo(maxGap) <= 0;
    }

    private boolean sameMetaId(MetricEvent left, MetricEvent right, String key) {
        Object a = left.getMeta() != null ? left.getMeta().get(key) : null;
        Object b = right.getMeta() != null ? right.getMeta().get(key) : null;
        return a != null && a.toString().equals(b != null ? b.toString() : null);
    }

    private String stringMeta(Map<String, Object> meta, String key, String fallback) {
        if (meta == null) return fallback;
        Object value = meta.get(key);
        return value != null && !value.toString().isBlank() ? value.toString() : fallback;
    }

    private boolean isLoginEvent(MetricEvent event) {
        return event.getEventType().equals("login_succeeded") || event.getEventType().equals("login_success")
                || event.getEventType().equals("user_registered");
    }

    private boolean isGroupRelationEvent(MetricEvent event) {
        return event.getEventType().equals("reading_group_created")
                || event.getEventType().equals("reading_group_joined")
                || event.getEventType().equals("reading_group_join_requested");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private User assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.isAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    private record GroupSequenceSignal(MetricEvent created, MetricEvent joined) {
    }
}
