package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricEventService {

    private final MetricEventRepository metricEventRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void record(MetricEventRequest request, String ip, Long userId) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        metricEventRepository.save(MetricEvent.builder()
                .user(user)
                .eventType(request.eventType())
                .sessionId(request.sessionId())
                .path(request.path())
                .referrer(request.referrer())
                .durationMs(Math.max(request.durationMs(), 0))
                .device(request.device())
                .ip(ip)
                .build());
    }
}
