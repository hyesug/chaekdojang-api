package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.Contest;
import com.chaekdojang.api.domain.contest.ContestEntryType;
import com.chaekdojang.api.domain.contest.ContestStatus;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;

import java.time.LocalDateTime;
import java.util.List;

public record ContestSummaryResponse(
        Long id,
        String title,
        ContestStatus status,
        ContestEntryType entryType,
        Long hostProfileId,
        String hostName,
        String hostSlug,
        OfficialProfileType hostType,
        boolean platformHosted,
        LocalDateTime submitStartAt,
        LocalDateTime submitEndAt,
        LocalDateTime announceAt,
        long entryCount,
        List<ContestBookResponse> books
) {
    public static ContestSummaryResponse of(Contest contest, long entryCount, List<ContestBookResponse> books) {
        return new ContestSummaryResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getStatus(),
                contest.getEntryType(),
                contest.getHost().getId(),
                contest.getHost().getDisplayName(),
                contest.getHost().getSlug(),
                contest.getHost().getType(),
                contest.isPlatformHosted(),
                contest.getSubmitStartAt(),
                contest.getSubmitEndAt(),
                contest.getAnnounceAt(),
                entryCount,
                books
        );
    }
}
