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
        LifeBook lifeBook
) {
    public record LibrarySummary(long readingCount, long finishedCount, long wishlistCount) {
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
            LibrarySummary librarySummary) {
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
                lifeBook
        );
    }
}
