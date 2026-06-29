package com.chaekdojang.api.domain.review.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummary;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryStatus;
import com.chaekdojang.api.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        AuthorInfo author,
        BookInfo book,
        AiSummaryInfo aiSummary,
        String content,
        int rating,
        boolean hidden,
        long viewCount,
        long likeCount,
        long commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record AuthorInfo(Long id, String nickname, String profileImage) {
        public static AuthorInfo from(User user) {
            if (user.getDeletedAt() != null) {
                return new AuthorInfo(null, "탈퇴한 사용자", null);
            }
            return new AuthorInfo(user.getId(), user.getNickname(), user.getProfileImage());
        }
    }

    public record BookInfo(Long id, String isbn13, String title, String author, String thumbnail) {
        public static BookInfo from(Book book) {
            return new BookInfo(book.getId(), book.getIsbn13(), book.getTitle(),
                    book.getAuthor(), book.getThumbnail());
        }
    }

    public static ReviewResponse from(Review review, long likeCount, long commentCount) {
        return from(review, likeCount, commentCount, null);
    }

    public static ReviewResponse from(
            Review review,
            long likeCount,
            long commentCount,
            ReviewAiSummary aiSummary
    ) {
        return new ReviewResponse(
                review.getId(),
                AuthorInfo.from(review.getAuthor()),
                review.getBook() != null ? BookInfo.from(review.getBook()) : null,
                AiSummaryInfo.from(aiSummary),
                review.getContent(),
                review.getRating(),
                review.isHidden(),
                review.getViewCount(),
                likeCount,
                commentCount,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    public record AiSummaryInfo(
            String oneLineReview,
            List<String> emotionKeywords,
            String recommendedFor,
            String impressivePoint
    ) {
        public static AiSummaryInfo from(ReviewAiSummary summary) {
            if (summary == null) return null;
            if (summary.getStatus() != ReviewAiSummaryStatus.COMPLETED
                    && summary.getStatus() != ReviewAiSummaryStatus.EDITED) {
                return null;
            }
            return new AiSummaryInfo(
                    summary.getOneLineReview(),
                    List.copyOf(summary.getEmotionKeywords()),
                    summary.getRecommendedFor(),
                    summary.getImpressivePoint()
            );
        }
    }
}
