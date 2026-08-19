package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.metrics.MetricEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VisitorIdentityResolver {

    private final Map<String, Set<Long>> userIdsByDevice;
    private final Map<String, Set<Long>> userIdsBySession;

    private VisitorIdentityResolver(
            Map<String, Set<Long>> userIdsByDevice,
            Map<String, Set<Long>> userIdsBySession) {
        this.userIdsByDevice = userIdsByDevice;
        this.userIdsBySession = userIdsBySession;
    }

    static VisitorIdentityResolver from(List<MetricEvent> events) {
        Map<String, Set<Long>> userIdsByDevice = new HashMap<>();
        Map<String, Set<Long>> userIdsBySession = new HashMap<>();

        events.forEach(event -> {
            Long userId = event.getUser() != null ? event.getUser().getId() : null;
            if (userId == null) return;
            addUser(userIdsByDevice, event.getDeviceId(), userId);
            addUser(userIdsBySession, event.getSessionId(), userId);
        });

        return new VisitorIdentityResolver(userIdsByDevice, userIdsBySession);
    }

    long count(List<MetricEvent> events) {
        return events.stream().map(this::key).distinct().count();
    }

    String key(MetricEvent event) {
        Long userId = event.getUser() != null ? event.getUser().getId() : null;
        if (userId != null) return "u:" + userId;

        Set<Long> linkedUserIds = new HashSet<>();
        addLinkedUsers(linkedUserIds, userIdsByDevice, event.getDeviceId());
        addLinkedUsers(linkedUserIds, userIdsBySession, event.getSessionId());
        if (linkedUserIds.size() == 1) return "u:" + linkedUserIds.iterator().next();

        if (hasText(event.getDeviceId())) return "d:" + event.getDeviceId();
        if (hasText(event.getSessionId())) return "s:" + event.getSessionId();
        if (hasText(event.getIp())) return "ip:" + event.getIp();
        return "event:" + (event.getId() != null ? event.getId() : System.identityHashCode(event));
    }

    private static void addUser(Map<String, Set<Long>> usersByValue, String value, Long userId) {
        if (!hasText(value)) return;
        usersByValue.computeIfAbsent(value, ignored -> new HashSet<>()).add(userId);
    }

    private static void addLinkedUsers(Set<Long> target, Map<String, Set<Long>> source, String value) {
        if (!hasText(value)) return;
        target.addAll(source.getOrDefault(value, Set.of()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
