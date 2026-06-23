package com.chaekdojang.api.global.traffic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AdminTrafficFilter {

    private final Set<String> excludedIps;

    public AdminTrafficFilter(@Value("${app.admin.excluded-ips:}") String excludedIps) {
        this.excludedIps = parse(excludedIps);
    }

    public boolean isExcludedIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String normalized = ip.trim();
        return excludedIps.contains(normalized)
                || excludedIps.contains(maskIp(normalized))
                || excludedIps.stream().anyMatch(excluded -> matchesWildcard(excluded, normalized));
    }

    public List<String> queryExcludedIps(List<String> dynamicIps) {
        Set<String> values = new LinkedHashSet<>();
        dynamicIps.stream()
                .filter(ip -> ip != null && !ip.isBlank())
                .forEach(ip -> {
                    values.add(ip.trim());
                    values.add(maskIp(ip.trim()));
                });
        excludedIps.forEach(ip -> {
            values.add(ip);
            if (!ip.endsWith(".*")) values.add(maskIp(ip));
        });
        return values.isEmpty() ? List.of("__no_admin_ip__") : values.stream().toList();
    }

    public String primaryExcludedIpPrefix(List<String> dynamicIps) {
        return queryExcludedIps(dynamicIps).stream()
                .map(this::toPrefix)
                .filter(prefix -> !prefix.isBlank())
                .findFirst()
                .orElse("");
    }

    private Set<String> parse(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean matchesWildcard(String excluded, String ip) {
        if (!excluded.endsWith(".*")) return false;
        return ip.startsWith(excluded.substring(0, excluded.length() - 1));
    }

    private String toPrefix(String ip) {
        if (ip == null || ip.isBlank()) return "";
        if (ip.endsWith(".*")) return ip.substring(0, ip.length() - 1);
        if (!ip.contains(".")) return "";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot <= 0) return "";
        String lastPart = ip.substring(lastDot + 1);
        return "0".equals(lastPart) ? ip.substring(0, lastDot + 1) : "";
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
