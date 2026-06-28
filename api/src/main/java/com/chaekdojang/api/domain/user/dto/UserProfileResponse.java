package com.chaekdojang.api.domain.user.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.user.User;

public record UserProfileResponse(
        Long id,
        String nickname,
        String bio,
        String profileImage,
        String role,
        long reviewCount,
        long followerCount,
        long followingCount,
        LibrarySummary librarySummary,
        ReadingGoalSummary readingGoal,
        LifeBook lifeBook,
        boolean onboardingCompleted,
        String preferredGenres
) {
    public record LibrarySummary(long readingCount, long finishedCount, long wishlistCount) {
    }

    public record ReadingGoalSummary(
            Integer year,
            Integer targetCount,
            long finishedCount,
            int progressPercent,
            long remainingCount,
            boolean publicVisible
    ) {
        public static ReadingGoalSummary of(Integer year, Integer targetCount, long finishedCount, boolean publicVisible) {
            if (year == null || targetCount == null || targetCount <= 0) return null;
            int progressPercent = (int) Math.min(100, Math.round((finishedCount * 100.0) / targetCount));
            long remainingCount = Math.max(0, targetCount - finishedCount);
            return new ReadingGoalSummary(year, targetCount, finishedCount, progressPercent, remainingCount, publicVisible);
        }
    }

    public record LifeBook(Long id, String title, String author, String thumbnail) {
        public static LifeBook from(Book book) {
            return new LifeBook(book.getId(), book.getTitle(), book.getAuthor(), book.getThumbnail());
        }
    }

    public static UserProfileResponse of(
            User user,
            long reviewCount,
            long followerCount,
            long followingCount,
            LibrarySummary librarySummary,
            ReadingGoalSummary readingGoal) {
        LifeBook lifeBook = user.getLifeBook() != null ? LifeBook.from(user.getLifeBook()) : null;
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getBio(),
                user.getProfileImage(),
                user.getRole().name(),
                reviewCount,
                followerCount,
                followingCount,
                librarySummary,
                readingGoal,
                lifeBook,
                user.isOnboardingCompleted(),
                user.getPreferredGenres()
        );
    }
}
