package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.ContestEntry;

/** 발표 후 공개되는 수상작 */
public record ContestAwardResponse(
        Long entryId,
        Integer awardRank,
        String awardName,
        String nickname,
        String entryTitle,
        String bookTitle,
        Long reviewId,
        String content
) {
    public static ContestAwardResponse from(ContestEntry entry) {
        return new ContestAwardResponse(
                entry.getId(),
                entry.getAwardRank(),
                entry.getAwardName(),
                entry.getUser().getNickname(),
                entry.getTitle(),
                entry.getBook() == null ? null : entry.getBook().getTitle(),
                entry.getReview() == null ? null : entry.getReview().getId(),
                entry.getContent()
        );
    }
}
