package com.chaekdojang.api.domain.admin.dto;

public record AdminDashboardSummaryResponse(
        long todayVisitors,
        long todayPageViews,
        long todayReviews,
        long todayUsers,
        long todayServerErrors,
        long todaySuspiciousRequests
) {
}
