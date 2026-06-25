package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupReview;
import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;

public record ReadingGroupReviewResponse(
        Long id,
        Long reviewId,
        Long authorId,
        String authorNickname,
        String authorProfileImage,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        String bookThumbnail,
        String content,
        int rating,
        long viewCount,
        LocalDateTime createdAt
) {
    public static ReadingGroupReviewResponse from(ReadingGroupReview groupReview) {
        Review review = groupReview.getReview();
        return new ReadingGroupReviewResponse(
                groupReview.getId(),
                review.getId(),
                review.getAuthor().getId(),
                review.getAuthor().getNickname(),
                review.getAuthor().getProfileImage(),
                review.getBook() != null ? review.getBook().getId() : null,
                review.getBook() != null ? review.getBook().getTitle() : null,
                review.getBook() != null ? review.getBook().getAuthor() : null,
                review.getBook() != null ? review.getBook().getThumbnail() : null,
                review.getContent(),
                review.getRating(),
                review.getViewCount(),
                review.getCreatedAt()
        );
    }
}
