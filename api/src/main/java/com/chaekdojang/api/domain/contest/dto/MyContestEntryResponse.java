package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.Contest;
import com.chaekdojang.api.domain.contest.ContestEntry;
import com.chaekdojang.api.domain.contest.ContestEntryStatus;
import com.chaekdojang.api.domain.contest.ContestEntryType;
import com.chaekdojang.api.domain.contest.ContestStatus;

import java.time.LocalDateTime;

/** 독자가 보는 내 응모 1건 */
public record MyContestEntryResponse(
        Long id,
        Long contestId,
        String contestTitle,
        String hostName,
        ContestStatus contestStatus,
        ContestEntryType entryType,
        ContestEntryStatus status,
        String entryTitle,
        Long bookId,
        String bookTitle,
        Long reviewId,
        Integer awardRank,
        String awardName,
        LocalDateTime submittedAt,
        LocalDateTime submitEndAt,
        LocalDateTime announceAt
) {
    public static MyContestEntryResponse from(ContestEntry entry) {
        Contest contest = entry.getContest();
        return new MyContestEntryResponse(
                entry.getId(),
                contest.getId(),
                contest.getTitle(),
                contest.getHost().getDisplayName(),
                contest.getStatus(),
                contest.getEntryType(),
                entry.getStatus(),
                entry.getTitle(),
                entry.getBook() == null ? null : entry.getBook().getId(),
                entry.getBook() == null ? null : entry.getBook().getTitle(),
                entry.getReview() == null ? null : entry.getReview().getId(),
                entry.getAwardRank(),
                entry.getAwardName(),
                entry.getSubmittedAt(),
                contest.getSubmitEndAt(),
                contest.getAnnounceAt()
        );
    }
}
