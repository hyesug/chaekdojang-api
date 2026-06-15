package com.chaekdojang.api.global.filter;

import com.chaekdojang.api.domain.accesslog.AccessLogService;
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

    // DB에 저장하지 않을 경로 접두사
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/oauth2/", "/login/oauth2/", "/actuator/", "/swagger", "/v3/api-docs"
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
            String ip = getClientIp(request);
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            log.info("[{}] {} | {} | {} | {}ms", method, uri, ip, status, elapsed);

            // OPTIONS, 노이즈 경로는 DB 저장 생략
            if (!"OPTIONS".equals(method) && !shouldSkip(uri)) {
                accessLogService.save(ip, method, uri, status, elapsed);
            }
        }
    }

    private boolean shouldSkip(String uri) {
        return SKIP_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
