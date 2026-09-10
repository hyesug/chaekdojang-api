package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.Contest;

import java.util.List;

public record ManageContestDetailResponse(
        ContestSummaryResponse contest,
        String description,
        String prizeDescription,
        long entryCount,
        long awardedCount,
        long notAwardedCount,
        long withdrawnCount
) {
    public static ManageContestDetailResponse of(Contest contest, List<ContestBookResponse> books,
                                                 long entryCount, long awardedCount,
                                                 long notAwardedCount, long withdrawnCount) {
        return new ManageContestDetailResponse(
                ContestSummaryResponse.of(contest, entryCount, books),
                contest.getDescription(),
                contest.getPrizeDescription(),
                entryCount,
                awardedCount,
                notAwardedCount,
                withdrawnCount
        );
    }
}
