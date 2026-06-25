package com.chaekdojang.api.domain.errorlog;

import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    @Transactional
    public void save(HttpServletRequest request, int status, Throwable error) {
        if (isExpectedAnonymousRefresh(request, status)) {
            return;
        }
        save(
                status >= 500 ? "ERROR" : "WARN",
                request.getMethod(),
                request.getRequestURI(),
                status,
                error.getClass().getSimpleName(),
                truncate(error.getMessage(), 1000),
                maskIp(getClientIp(request)),
                currentUserIdOrNull(),
                truncate(request.getHeader("User-Agent"), 500),
                truncate(request.getHeader("Referer"), 500)
        );
    }

    private boolean isExpectedAnonymousRefresh(HttpServletRequest request, int status) {
        return status == 403
                && "POST".equals(request.getMethod())
                && "/api/auth/refresh".equals(request.getRequestURI());
    }

    public void save(String level, String method, String uri, int status,
                     String exceptionType, String message, String ip, Long userId,
                     String userAgent, String referer) {
        errorLogRepository.save(ErrorLog.builder()
                .level(level)
                .method(method)
                .uri(uri)
                .status(status)
                .exceptionType(exceptionType)
                .message(message)
                .ip(ip)
                .userId(userId)
                .userAgent(userAgent)
                .referer(referer)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<ErrorLog> search(
            String q,
            String level,
            Integer statusMin,
            Integer statusMax,
            Pageable pageable) {
        return errorLogRepository.search(q, level, statusMin, statusMax, pageable);
    }

    private Long currentUserIdOrNull() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) return "(no message)";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
