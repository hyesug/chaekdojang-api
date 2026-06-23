package com.chaekdojang.api.domain.accesslog;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    /**
     * 비동기로 저장 — 요청 처리 지연 없음.
     * @EnableAsync가 활성화되어 있어야 동작함 (AppConfig 참고).
     */
    @Async
    @Transactional
    public void save(String ip, String method, String uri, int status, long elapsedMs) {
        accessLogRepository.save(AccessLog.builder()
                .ip(ip)
                .method(method)
                .uri(uri)
                .status(status)
                .elapsedMs(elapsedMs)
                .build());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AccessLog> search(
            String q,
            String method,
            Integer statusMin,
            Integer statusMax,
            java.util.List<String> excludedIps,
            org.springframework.data.domain.Pageable pageable) {
        return accessLogRepository.search(q, method, statusMin, statusMax, excludedIps, pageable);
    }
}
