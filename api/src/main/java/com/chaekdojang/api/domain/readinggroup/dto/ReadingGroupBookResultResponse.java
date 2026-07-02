package com.chaekdojang.api.domain.readinggroup.dto;

import java.util.List;

public record ReadingGroupBookResultResponse(
        String groupName,
        String groupSlug,
        BookInfo book,
        long participantCount,
        long reviewCount,
        double averageRating,
        List<String> commonEmotionKeywords,
        String representativeOneLineReview,
        String recommendedForSummary,
        String impressivePointSummary,
        long publicReviewCount,
        long generatedCardCount,
        List<AiReadingCardInfo> cards
) {
    public record BookInfo(
            Long id,
            String title,
            String author,
            String thumbnail
    ) {
    }

    public record AiReadingCardInfo(
            Long reviewId,
            String authorNickname,
            String bookTitle,
            String oneLineReview,
            List<String> emotionKeywords,
            String recommendedFor,
            String impressivePoint,
            int rating
    ) {
    }
}
