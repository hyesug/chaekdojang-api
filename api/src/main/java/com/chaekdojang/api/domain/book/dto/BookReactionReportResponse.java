package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummary;

import java.util.List;

public record BookReactionReportResponse(
        BookInfo book,
        long reviewCount,
        long participantCount,
        double averageRating,
        List<String> commonEmotionKeywords,
        String representativeOneLineReview,
        String recommendedForSummary,
        String impressivePointSummary,
        List<ReviewCardInfo> cards
) {
    public record BookInfo(
            Long id,
            String title,
            String author,
            String thumbnail
    ) {
        public static BookInfo from(Book book) {
            return new BookInfo(book.getId(), book.getTitle(), book.getAuthor(), book.getThumbnail());
        }
    }

    public record ReviewCardInfo(
            Long reviewId,
            String authorNickname,
            String oneLineReview,
            List<String> emotionKeywords,
            String recommendedFor,
            String impressivePoint,
            int rating
    ) {
        public static ReviewCardInfo of(Review review, ReviewAiSummary summary) {
            return new ReviewCardInfo(
                    review.getId(),
                    review.getAuthor().getDeletedAt() == null ? review.getAuthor().getNickname() : "탈퇴한 사용자",
                    summary.getOneLineReview(),
                    List.copyOf(summary.getEmotionKeywords()),
                    summary.getRecommendedFor(),
                    summary.getImpressivePoint(),
                    review.getRating()
            );
        }
    }
}
