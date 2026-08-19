package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.metrics.MetricEvent;
import com.chaekdojang.api.domain.metrics.UserAgentInfo;
import com.chaekdojang.api.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitorIdentityResolverTest {

    @Test
    void guestAndLoggedInEventsOnSameDeviceCountAsOneVisitor() {
        MetricEvent guest = event(1L, null, "guest-session", "device-a", "203.0.113.1");
        MetricEvent loggedIn = event(2L, 7L, "member-session", "device-a", "203.0.113.1");
        List<MetricEvent> events = List.of(guest, loggedIn);

        assertThat(VisitorIdentityResolver.from(events).count(events)).isEqualTo(1);
    }

    @Test
    void guestAndLoggedInEventsWithSameSessionCountAsOneVisitor() {
        MetricEvent guest = event(1L, null, "shared-session", null, "203.0.113.1");
        MetricEvent loggedIn = event(2L, 7L, "shared-session", null, "203.0.113.1");
        List<MetricEvent> events = List.of(guest, loggedIn);

        assertThat(VisitorIdentityResolver.from(events).count(events)).isEqualTo(1);
    }

    @Test
    void multipleGuestSessionsOnSameDeviceCountAsOneVisitor() {
        List<MetricEvent> events = List.of(
                event(1L, null, "session-a", "device-a", "203.0.113.1"),
                event(2L, null, "session-b", "device-a", "203.0.113.1")
        );

        assertThat(VisitorIdentityResolver.from(events).count(events)).isEqualTo(1);
    }

    @Test
    void differentLoggedInUsersOnSharedDeviceRemainSeparate() {
        List<MetricEvent> events = List.of(
                event(1L, 7L, "session-a", "shared-device", "203.0.113.1"),
                event(2L, 8L, "session-b", "shared-device", "203.0.113.1")
        );

        assertThat(VisitorIdentityResolver.from(events).count(events)).isEqualTo(2);
    }

    @Test
    void detectsKnownCrawlerAndAutomationUserAgents() {
        assertThat(UserAgentInfo.isBot("Mozilla/5.0 (compatible; Googlebot/2.1)")).isTrue();
        assertThat(UserAgentInfo.isBot("Mozilla/5.0 HeadlessChrome/140.0")).isTrue();
        assertThat(UserAgentInfo.isBot("Mozilla/5.0 Chrome/140.0 Safari/537.36")).isFalse();
    }

    private MetricEvent event(Long id, Long userId, String sessionId, String deviceId, String ip) {
        MetricEvent event = mock(MetricEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getSessionId()).thenReturn(sessionId);
        when(event.getDeviceId()).thenReturn(deviceId);
        when(event.getIp()).thenReturn(ip);
        if (userId != null) {
            User user = mock(User.class);
            when(user.getId()).thenReturn(userId);
            when(event.getUser()).thenReturn(user);
        }
        return event;
    }
}
