package com.chaekdojang.api.domain.readinggroup.dto;

import java.util.List;

public record ReadingGroupBookResultResponse(
        String groupName,
        String groupSlug,
        BookInfo book,
        long participantCount,
        List<String> commonEmotionKeywords,
        String representativeOneLineReview,
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
            String impressivePoint
    ) {
    }
}
