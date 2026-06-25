package com.chaekdojang.api.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int apiLimitPerMinute;
    private final int uploadLimitPerMinute;
    private final int authLimitPerMinute;
    private final int authRefreshLimitPerMinute;
    private final int metricsLimitPerMinute;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.api-limit-per-minute:600}") int apiLimitPerMinute,
            @Value("${app.rate-limit.upload-limit-per-minute:20}") int uploadLimitPerMinute,
            @Value("${app.rate-limit.auth-limit-per-minute:30}") int authLimitPerMinute,
            @Value("${app.rate-limit.auth-refresh-limit-per-minute:120}") int authRefreshLimitPerMinute,
            @Value("${app.rate-limit.metrics-limit-per-minute:240}") int metricsLimitPerMinute
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.apiLimitPerMinute = apiLimitPerMinute;
        this.uploadLimitPerMinute = uploadLimitPerMinute;
        this.authLimitPerMinute = authLimitPerMinute;
        this.authRefreshLimitPerMinute = authRefreshLimitPerMinute;
        this.metricsLimitPerMinute = metricsLimitPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitPolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate:" + policy.bucket + ":" + minuteWindow() + ":" + clientIp(request);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(90));
            }
            if (count != null && count > policy.limitPerMinute) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
                return;
            }
        } catch (Exception ignored) {
            // Redis 장애 때문에 정상 요청을 막지 않도록 통과시킨다.
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = normalizedPath(request);
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.startsWith("/uploads/");
    }

    private LimitPolicy resolvePolicy(HttpServletRequest request) {
        String path = normalizedPath(request);
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        if (path.startsWith("/api/metrics/events")) {
            return new LimitPolicy("metrics", metricsLimitPerMinute);
        }
        if (path.startsWith("/api/upload") || path.contains("/profile-image")) {
            return new LimitPolicy("upload", uploadLimitPerMinute);
        }
        if (path.equals("/api/auth/session")) {
            return null;
        }
        if (path.equals("/api/auth/refresh")) {
            return new LimitPolicy("auth-refresh", authRefreshLimitPerMinute);
        }
        if (path.startsWith("/api/auth")
                || path.startsWith("/oauth2")
                || path.startsWith("/login/oauth2")
                || path.startsWith("/api/dev/login")) {
            return new LimitPolicy("auth", authLimitPerMinute);
        }
        if (path.startsWith("/api/books/search")) {
            return new LimitPolicy("search", Math.min(apiLimitPerMinute, 120));
        }
        if (path.matches("^/api/reviews/\\d+/comments.*") && (method.equals("POST") || method.equals("DELETE"))) {
            return new LimitPolicy("comments", Math.min(apiLimitPerMinute, 60));
        }
        if (path.startsWith("/api/")) {
            return new LimitPolicy("api", apiLimitPerMinute);
        }
        return null;
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) return "/";
        return path.trim();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private long minuteWindow() {
        return Instant.now().getEpochSecond() / 60;
    }

    private record LimitPolicy(String bucket, int limitPerMinute) {
    }
}
