package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetricEventService {

    public static final String DEVICE_COOKIE = "chaekdojang_device_id";
    public static final String SESSION_COOKIE = "chaekdojang_session_id";
    private static final String KAKAO_GROUP_INVITE_SOURCE = "kakao_reading_group";
    private static final String COPY_GROUP_INVITE_SOURCE = "reading_group_link";
    private static final String KAKAO_GROUP_INVITE_REFERRER = "chaekdojang://invite/kakao-reading-group";
    private static final String COPY_GROUP_INVITE_REFERRER = "chaekdojang://invite/reading-group-link";
    private static final Set<String> CLIENT_EVENT_TYPES = Set.of(
            "page_view", "review_write_click", "book_search", "book_click_search",
            "web_novel_search", "share_click", "heartbeat", "session_end", "revision_saved"
    );
    private static final Set<String> SENSITIVE_META_PARTS = Set.of(
            "token", "password", "secret", "content", "body", "prompt", "authorization"
    );

    private final MetricEventRepository metricEventRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void record(MetricEventRequest request, String ip, String userAgent, Long userId) {
        if (!CLIENT_EVENT_TYPES.contains(request.eventType())) return;
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user != null && user.isAdmin()) {
            return;
        }

        UserAgentInfo agent = UserAgentInfo.parse(userAgent);
        metricEventRepository.save(MetricEvent.builder()
                .user(user)
                .eventType(request.eventType())
                .sessionId(request.sessionId())
                .path(request.path())
                .referrer(resolveClientReferrer(request))
                .durationMs(Math.max(request.durationMs(), 0))
                .device(normalize(request.device(), agent.device(), 80))
                .deviceId(normalize(request.deviceId(), null, 80))
                .userAgent(normalize(userAgent, null, 1000))
                .browser(agent.browser())
                .operatingSystem(agent.operatingSystem())
                .ip(ip)
                .meta(sanitizeClientMeta(request.meta()))
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemEvent(String eventType, String sessionId, String path, String ip, Long userId, Map<String, Object> meta) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user != null && user.isAdmin()) {
            return;
        }

        metricEventRepository.save(MetricEvent.builder()
                .user(user)
                .eventType(eventType)
                .sessionId(sessionId)
                .path(path)
                .referrer(null)
                .durationMs(0)
                .device(null)
                .deviceId(null)
                .userAgent(null)
                .browser(null)
                .operatingSystem(null)
                .ip(ip)
                .meta(meta)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCurrentRequestEvent(String eventType, Long userId, String path, Map<String, Object> meta) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        recordRequestEvent(eventType, userId, path, meta, request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequestEvent(String eventType, Long userId, String path, Map<String, Object> meta,
                                   HttpServletRequest request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isAdmin()) return;

        String ip = request != null ? ClientIpUtils.getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        UserAgentInfo agent = UserAgentInfo.parse(userAgent);

        metricEventRepository.save(MetricEvent.builder()
                .user(user)
                .eventType(eventType)
                .sessionId(cookieOrHeader(request, SESSION_COOKIE, "X-Chaekdojang-Session-Id",
                        "server-" + UUID.randomUUID()))
                .path(normalize(path, "/", 500))
                .referrer(request != null ? normalize(request.getHeader("Referer"), null, 500) : null)
                .durationMs(0)
                .device(agent.device())
                .deviceId(cookieOrHeader(request, DEVICE_COOKIE, "X-Chaekdojang-Device-Id", null))
                .userAgent(normalize(userAgent, null, 1000))
                .browser(agent.browser())
                .operatingSystem(agent.operatingSystem())
                .ip(normalize(ip, null, 50))
                .meta(meta == null ? Map.of() : Map.copyOf(meta))
                .build());
    }

    private Map<String, Object> sanitizeClientMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return Map.of();
        Map<String, Object> sanitized = new LinkedHashMap<>();
        meta.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.toLowerCase();
            if (normalizedKey.isBlank()
                    || SENSITIVE_META_PARTS.stream().anyMatch(normalizedKey::contains)
                    || sanitized.size() >= 20) return;
            if (value instanceof Number || value instanceof Boolean) {
                sanitized.put(key, value);
            } else if (value instanceof String text) {
                sanitized.put(key, text.substring(0, Math.min(text.length(), 200)));
            }
        });
        return Map.copyOf(sanitized);
    }

    private String resolveClientReferrer(MetricEventRequest request) {
        Object inviteSource = request.meta() != null ? request.meta().get("inviteSource") : null;
        if (request.path().startsWith("/groups/") && KAKAO_GROUP_INVITE_SOURCE.equals(inviteSource)) {
            return KAKAO_GROUP_INVITE_REFERRER;
        }
        if (request.path().startsWith("/groups/") && COPY_GROUP_INVITE_SOURCE.equals(inviteSource)) {
            return COPY_GROUP_INVITE_REFERRER;
        }
        return normalize(request.referrer(), null, 500);
    }

    private String cookieOrHeader(HttpServletRequest request, String cookieName, String headerName, String fallback) {
        if (request == null) return fallback;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return normalize(cookie.getValue(), fallback, 80);
                }
            }
        }
        return normalize(request.getHeader(headerName), fallback, 80);
    }

    private String normalize(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }
}
