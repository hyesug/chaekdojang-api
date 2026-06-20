package com.chaekdojang.api.global.filter;

import com.chaekdojang.api.config.RateLimitProperties;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_COUNTERS = 50_000;

    private final RateLimitProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) return true;
        String uri = request.getRequestURI();
        return "OPTIONS".equals(request.getMethod())
                || uri.startsWith("/actuator/")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String policy = resolvePolicy(request);
        int limit = resolveLimit(policy);
        String key = policy + ":" + ClientIpUtils.getClientIp(request);

        if (!tryConsume(key, limit)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolvePolicy(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/upload/")) return "upload";
        if (uri.startsWith("/api/metrics/events")) return "metrics";
        if (uri.startsWith("/api/dev/login") || uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/")) {
            return "auth";
        }
        if (uri.startsWith("/api/")) return "api";
        return "page";
    }

    private int resolveLimit(String policy) {
        return switch (policy) {
            case "upload" -> properties.getUploadLimitPerMinute();
            case "metrics" -> properties.getMetricsLimitPerMinute();
            case "auth" -> properties.getAuthLimitPerMinute();
            default -> properties.getApiLimitPerMinute();
        };
    }

    private boolean tryConsume(String key, int limit) {
        long now = clock.millis();
        long windowStart = now - (now % WINDOW_MILLIS);
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStart != windowStart) {
                return new WindowCounter(windowStart);
            }
            return existing;
        });

        int used = counter.count.incrementAndGet();
        if (counters.size() > MAX_COUNTERS) {
            counters.entrySet().removeIf(entry -> entry.getValue().windowStart < windowStart);
        }
        return used <= limit;
    }

    private static final class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
