package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;
import java.util.List;

public record PublicBookDetailResponse(
        Long id,
        String isbn13,
        String title,
        String author,
        String publisher,
        String thumbnail,
        String slug,
        String description,
        Integer publishedYear,
        String seoTitle,
        String seoDescription,
        long reviewCount,
        long readerCount,
        LocalDateTime updatedAt,
        List<ReviewExcerpt> reviewExcerpts,
        List<String> sentenceExcerpts
) {
    public record ReviewExcerpt(
            Long id,
            String authorNickname,
            String content,
            int rating,
            long likeCount,
            long commentCount,
            LocalDateTime createdAt
    ) {
        public static ReviewExcerpt from(Review review, long likeCount, long commentCount) {
            return new ReviewExcerpt(
                    review.getId(),
                    review.getAuthor().getDeletedAt() == null ? review.getAuthor().getNickname() : "탈퇴한 사용자",
                    review.getContent(),
                    review.getRating(),
                    likeCount,
                    commentCount,
                    review.getCreatedAt()
            );
        }
    }

    public static PublicBookDetailResponse from(
            Book book,
            long reviewCount,
            long readerCount,
            List<ReviewExcerpt> reviewExcerpts,
            List<String> sentenceExcerpts
    ) {
        return new PublicBookDetailResponse(
                book.getId(),
                book.getIsbn13(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getThumbnail(),
                book.getSlug(),
                book.getDescription(),
                book.getPublishedYear(),
                book.getSeoTitle(),
                book.getSeoDescription(),
                reviewCount,
                readerCount,
                book.getUpdatedAt(),
                reviewExcerpts,
                sentenceExcerpts
        );
    }
}
