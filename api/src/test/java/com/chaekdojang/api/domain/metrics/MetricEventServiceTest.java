package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.traffic.AdminTrafficFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricEventServiceTest {

    @Mock MetricEventRepository metricEventRepository;
    @Mock UserRepository userRepository;
    @Mock AdminTrafficFilter adminTrafficFilter;
    @InjectMocks MetricEventService metricEventService;

    @Test
    void loginEventLinksUserAndRequestContext() {
        User user = user(7L, "reader");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0 Safari/537.36");
        request.setCookies(
                new Cookie(MetricEventService.SESSION_COOKIE, "session-1"),
                new Cookie(MetricEventService.DEVICE_COOKIE, "device-1")
        );

        metricEventService.recordRequestEvent(
                "login_succeeded", 7L, "/auth/callback", Map.of("oauthProvider", "GOOGLE"), request);

        ArgumentCaptor<MetricEvent> captor = ArgumentCaptor.forClass(MetricEvent.class);
        verify(metricEventRepository).save(captor.capture());
        MetricEvent event = captor.getValue();
        assertThat(event.getUser().getId()).isEqualTo(7L);
        assertThat(event.getSessionId()).isEqualTo("session-1");
        assertThat(event.getDeviceId()).isEqualTo("device-1");
        assertThat(event.getIp()).isEqualTo("203.0.113.10");
        assertThat(event.getBrowser()).isEqualTo("Chrome");
        assertThat(event.getOperatingSystem()).isEqualTo("Windows");
        assertThat(event.getMeta()).containsEntry("oauthProvider", "GOOGLE");
    }

    @Test
    void clientMetricRejectsServerEventAndSensitiveMeta() {
        MetricEventRequest forged = new MetricEventRequest(
                "reading_group_created", "session", "/groups/fake", null, 0,
                "desktop", "device", Map.of("groupId", 1));
        metricEventService.record(forged, "203.0.113.10", "test", null);
        verify(metricEventRepository, never()).save(any());

        MetricEventRequest safe = new MetricEventRequest(
                "revision_saved", "session", "/write", null, 0,
                "desktop", "device", Map.of(
                        "reviewId", 10,
                        "content", "독후감 본문",
                        "accessToken", "secret-token",
                        "prompt", "internal prompt"
                ));
        metricEventService.record(safe, "203.0.113.10", "test", null);

        ArgumentCaptor<MetricEvent> captor = ArgumentCaptor.forClass(MetricEvent.class);
        verify(metricEventRepository).save(captor.capture());
        assertThat(captor.getValue().getMeta()).containsOnlyKeys("reviewId");
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@example.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
