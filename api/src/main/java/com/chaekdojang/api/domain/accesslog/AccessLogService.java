package com.chaekdojang.api.domain.accesslog;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final UserRepository userRepository;

    /**
     * 비동기로 저장 — 요청 처리 지연 없음.
     * @EnableAsync가 활성화되어 있어야 동작함 (AppConfig 참고).
     */
    @Async
    @Transactional
    public void save(String ip, Long userId, String method, String uri, int status, long elapsedMs,
                     String userAgent, String deviceId) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user != null && user.isAdmin()) return;
        accessLogRepository.save(AccessLog.builder()
                .ip(ip)
                .user(user)
                .method(method)
                .uri(uri)
                .status(status)
                .elapsedMs(elapsedMs)
                .userAgent(trim(userAgent, 1000))
                .deviceId(trim(deviceId, 80))
                .build());
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AccessLog> search(
            String q,
            String method,
            Integer statusMin,
            Integer statusMax,
            org.springframework.data.domain.Pageable pageable) {
        return accessLogRepository.search(q, method, statusMin, statusMax, pageable);
    }
}
