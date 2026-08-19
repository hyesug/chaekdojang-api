package com.chaekdojang.api.domain.admin.dto;

public record AdminDashboardSummaryResponse(
        long todayVisitors,
        long todayBotVisitors,
        long todayPageViews,
        long todayBookSearches,
        long todayBookDetailViews,
        long todayReviewDetailViews,
        long todayReviews,
        long todayUsers,
        long todayServerErrors,
        long todaySuspiciousRequests
) {
}
