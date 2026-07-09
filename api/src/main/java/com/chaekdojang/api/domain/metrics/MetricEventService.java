package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.traffic.AdminTrafficFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricEventService {

    private final MetricEventRepository metricEventRepository;
    private final UserRepository userRepository;
    private final AdminTrafficFilter adminTrafficFilter;

    @Async
    @Transactional
    public void record(MetricEventRequest request, String ip, Long userId) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if ((user != null && user.isAdmin()) || adminTrafficFilter.isExcludedIp(ip)) {
            return;
        }

        metricEventRepository.save(MetricEvent.builder()
                .user(user)
                .eventType(request.eventType())
                .sessionId(request.sessionId())
                .path(request.path())
                .referrer(request.referrer())
                .durationMs(Math.max(request.durationMs(), 0))
                .device(request.device())
                .ip(ip)
                .meta(request.meta())
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemEvent(String eventType, String sessionId, String path, String ip, Long userId, Map<String, Object> meta) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if ((user != null && user.isAdmin()) || adminTrafficFilter.isExcludedIp(ip)) {
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
                .ip(ip)
                .meta(meta)
                .build());
    }
}
