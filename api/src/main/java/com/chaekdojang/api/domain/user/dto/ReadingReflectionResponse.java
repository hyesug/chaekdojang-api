package com.chaekdojang.api.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReadingReflectionResponse(
        int totalReviews,
        int rereadCount,
        int currentYearBookCount,
        double averageWritingIntervalDays,
        List<MonthlyReviewCount> monthlyReviews,
        List<YearlyBookCount> yearlyBooks,
        List<GenreByYear> genreTimeline,
        List<KeywordCount> frequentKeywords,
        List<RereadBook> rereadBooks,
        LongestRecordedBook longestRecordedBook,
        List<MemoryReview> memories,
        List<String> reflectionMessages
) {
    public record MonthlyReviewCount(int year, int month, int count) {}
    public record YearlyBookCount(int year, int count) {}
    public record GenreByYear(int year, String genre, int count) {}
    public record KeywordCount(String keyword, int count) {}
    public record RereadBook(Long bookId, String title, int recordCount, LocalDateTime firstAt, LocalDateTime latestAt) {}
    public record LongestRecordedBook(Long bookId, String title, long days, LocalDateTime firstAt, LocalDateTime latestAt) {}
    public record MemoryReview(Long reviewId, Long bookId, String bookTitle, int rating, LocalDateTime createdAt) {}
}
