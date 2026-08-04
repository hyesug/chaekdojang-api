package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.accesslog.AccessLogRepository;
import com.chaekdojang.api.domain.accesslog.AccessLog;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserActivityServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserAuthProviderRepository authProviderRepository;
    @Mock MetricEventRepository metricEventRepository;
    @Mock AccessLogRepository accessLogRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReadingGroupRepository readingGroupRepository;
    @Mock ReadingGroupMemberRepository readingGroupMemberRepository;
    @InjectMocks AdminUserActivityService service;

    private User admin;
    private User target;
    private User candidate;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin");
        admin.promoteToAdmin();
        target = user(2L, "first");
        candidate = user(3L, "second");
        ReflectionTestUtils.setField(service, "retentionDays", 90);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        lenient().when(metricEventRepository.findUserTimeline(eq(2L), anyString(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());
        lenient().when(authProviderRepository.findAllByUserIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());
    }

    @Test
    void sameDeviceIsHighRelatedSignal() {
        MetricEvent targetEvent = event(target, "page_view", "device-a", "203.0.113.10", "Chrome", LocalDateTime.now().minusMinutes(5));
        MetricEvent candidateEvent = event(candidate, "page_view", "device-a", "198.51.100.20", "Firefox", LocalDateTime.now());
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(List.of(targetEvent));
        when(metricEventRepository.findRelatedByDeviceIds(eq(2L), anyList(), any()))
                .thenReturn(List.of(candidateEvent));
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(3L), any()))
                .thenReturn(List.of(candidateEvent));

        AdminUserActivityResponse response = service.getUserActivity(
                1L, 2L, "", null, null, PageRequest.of(0, 20));

        assertThat(response.relatedAccounts()).hasSize(1);
        assertThat(response.relatedAccounts().get(0).strength()).isEqualTo("높음");
        assertThat(response.relatedAccounts().get(0).score()).isGreaterThanOrEqualTo(60);
        assertThat(response.relatedAccounts().get(0).reasons()).anyMatch(reason -> reason.contains("동일 기기 ID"));
    }

    @Test
    void sameIpOnlyIsLowSignal() {
        MetricEvent targetEvent = event(target, "page_view", null, "203.0.113.10", "Chrome", LocalDateTime.now().minusHours(2));
        MetricEvent candidateEvent = event(candidate, "page_view", null, "203.0.113.10", "Firefox", LocalDateTime.now());
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(List.of(targetEvent));
        when(metricEventRepository.findRelatedByIps(eq(2L), anyList(), any()))
                .thenReturn(List.of(candidateEvent));
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(3L), any()))
                .thenReturn(List.of(candidateEvent));

        AdminUserActivityResponse response = service.getUserActivity(
                1L, 2L, "", null, null, PageRequest.of(0, 20));

        assertThat(response.relatedAccounts()).hasSize(1);
        assertThat(response.relatedAccounts().get(0).strength()).isEqualTo("낮음");
        assertThat(response.relatedAccounts().get(0).score()).isEqualTo(5);
    }

    @Test
    void groupJoinSoonAfterCreationIsShownWithoutSharedDeviceOrIp() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(8);
        MetricEvent created = event(target, "reading_group_created", null, "203.0.113.10", "Chrome", createdAt,
                Map.of("groupId", 10L, "groupName", "테스트 모임"));
        MetricEvent joined = event(candidate, "reading_group_joined", null, "198.51.100.20", "Firefox",
                createdAt.plusMinutes(4), Map.of("groupId", 10L, "groupName", "테스트 모임"));
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(List.of(created));
        when(metricEventRepository.findRelatedByIps(eq(2L), anyList(), any())).thenReturn(List.of());
        when(metricEventRepository.findRelatedGroupActivity(eq(2L), any())).thenReturn(List.of(joined));
        when(metricEventRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(3L), any()))
                .thenReturn(List.of(joined));

        AdminUserActivityResponse response = service.getUserActivity(
                1L, 2L, "", null, null, PageRequest.of(0, 20));

        assertThat(response.relatedAccounts()).hasSize(1);
        assertThat(response.relatedAccounts().get(0).score()).isEqualTo(10);
        assertThat(response.relatedAccounts().get(0).reasons())
                .contains("「테스트 모임」 모임 생성 4분 후 다른 계정 가입");
    }

    @Test
    void nonAdminCannotReadActivity() {
        User normalUser = user(9L, "normal");
        when(userRepository.findById(9L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> service.getUserActivity(
                9L, 2L, "", null, null, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void repeatedPublicReadsCreateReferenceOnlyAlertAtConfiguredThreshold() {
        ReflectionTestUtils.setField(service, "publicReadAlertWindowMinutes", 5);
        ReflectionTestUtils.setField(service, "publicReadAlertThreshold", 2);
        AccessLog first = accessLog(target, "/api/books/public/10", LocalDateTime.now().minusMinutes(2));
        AccessLog second = accessLog(target, "/api/reviews/20", LocalDateTime.now().minusMinutes(1));
        when(accessLogRepository.findRecentPublicReads(any())).thenReturn(List.of(first, second));

        List<AdminPublicReadAlertResponse> alerts = service.getPublicReadAlerts(1L);

        assertThat(alerts).hasSize(2);
        assertThat(alerts).allSatisfy(alert -> {
            assertThat(alert.requestCount()).isEqualTo(2);
            assertThat(alert.message()).contains("자동 제재 근거로 사용하지 마세요");
        });
        assertThat(alerts).extracting(AdminPublicReadAlertResponse::actorKey)
                .containsExactlyInAnyOrder("ip:203.0.113.0", "user:2");
    }

    private MetricEvent event(User user, String eventType, String deviceId, String ip,
                              String browser, LocalDateTime createdAt) {
        return event(user, eventType, deviceId, ip, browser, createdAt, Map.of());
    }

    private MetricEvent event(User user, String eventType, String deviceId, String ip,
                              String browser, LocalDateTime createdAt, Map<String, Object> meta) {
        MetricEvent event = MetricEvent.builder()
                .user(user)
                .eventType(eventType)
                .sessionId("session")
                .path("/")
                .durationMs(0)
                .device("desktop")
                .deviceId(deviceId)
                .userAgent(browser)
                .browser(browser)
                .operatingSystem("Windows")
                .ip(ip)
                .meta(meta)
                .build();
        ReflectionTestUtils.setField(event, "createdAt", createdAt);
        return event;
    }

    private AccessLog accessLog(User user, String uri, LocalDateTime createdAt) {
        AccessLog log = AccessLog.builder()
                .ip("203.0.113.0")
                .user(user)
                .method("GET")
                .uri(uri)
                .status(200)
                .elapsedMs(10)
                .userAgent("Chrome")
                .deviceId("device-a")
                .build();
        ReflectionTestUtils.setField(log, "createdAt", createdAt);
        return log;
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@example.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now().minusDays(10));
        return user;
    }
}
