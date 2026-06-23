package com.chaekdojang.api.global.filter;

import com.chaekdojang.api.domain.accesslog.AccessLogService;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/oauth2/", "/login/oauth2/", "/actuator/", "/swagger", "/v3/api-docs",
            "/_next/", "/static/", "/assets/", "/images/", "/favicon", "/robots.txt",
            "/sitemap.xml", "/health", "/api/metrics/events", "/api/admin", "/api/admin/"
    );

    private static final Set<String> SKIP_SUFFIXES = Set.of(
            ".css", ".js", ".map", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".webp",
            ".svg", ".woff", ".woff2", ".ttf", ".txt", ".xml"
    );

    private final AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String ip = maskIp(ClientIpUtils.getClientIp(request));
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            log.info("[{}] {} | {} | {} | {}ms", method, uri, ip, status, elapsed);

            if (shouldSave(request, method, uri)) {
                accessLogService.save(ip, method, uri, status, elapsed);
            }
        }
    }

    private boolean shouldSave(HttpServletRequest request, String method, String uri) {
        if ("OPTIONS".equals(method)) return false;
        if (isInternalFrontendRequest(request)) return false;
        if (shouldSkip(uri)) return false;
        return true;
    }

    private boolean isInternalFrontendRequest(HttpServletRequest request) {
        return "web-ssr".equals(request.getHeader("X-Chaekdojang-Internal-Request"));
    }

    private boolean shouldSkip(String uri) {
        String lower = uri.toLowerCase();
        return SKIP_PREFIXES.stream().anyMatch(uri::startsWith)
                || SKIP_SUFFIXES.stream().anyMatch(lower::endsWith)
                || uri.contains("/opengraph-image");
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
}
