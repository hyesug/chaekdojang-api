package com.chaekdojang.api.domain.admin.dto;

import com.chaekdojang.api.domain.user.AuthProvider;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminUserActivityResponse(
        BasicInfo basic,
        AccessInfo access,
        Page<TimelineEvent> timeline,
        List<RelatedAccount> relatedAccounts,
        String notice
) {
    public record BasicInfo(
            Long id,
            String nickname,
            LocalDateTime createdAt,
            List<AuthProvider> oauthProviders,
            long reviewCount,
            long createdGroupCount,
            long joinedGroupCount
    ) {}

    public record AccessInfo(
            LocalDateTime firstLoginAt,
            LocalDateTime recentLoginAt,
            LocalDateTime recentActivityAt,
            String recentIp,
            String recentDevice,
            String recentBrowser,
            String recentOperatingSystem,
            List<String> deviceIds
    ) {}

    public record TimelineEvent(
            Long id,
            String eventType,
            String label,
            String description,
            LocalDateTime createdAt,
            String ip,
            String deviceId,
            String device,
            String browser,
            String operatingSystem,
            Map<String, Object> meta
    ) {}

    public record RelatedAccount(
            Long userId,
            String nickname,
            int score,
            String strength,
            List<String> reasons,
            LocalDateTime lastRelatedAt
    ) {}
}
