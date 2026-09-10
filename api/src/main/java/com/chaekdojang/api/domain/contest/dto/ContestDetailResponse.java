package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.Contest;

import java.util.List;

public record ContestDetailResponse(
        ContestSummaryResponse contest,
        String description,
        String prizeDescription,
        boolean acceptingEntries,
        MyContestEntryResponse myEntry,
        List<ContestAwardResponse> awards
) {
    public static ContestDetailResponse of(Contest contest, long entryCount, List<ContestBookResponse> books,
                                           boolean acceptingEntries, MyContestEntryResponse myEntry,
                                           List<ContestAwardResponse> awards) {
        return new ContestDetailResponse(
                ContestSummaryResponse.of(contest, entryCount, books),
                contest.getDescription(),
                contest.getPrizeDescription(),
                acceptingEntries,
                myEntry,
                awards
        );
    }
}
