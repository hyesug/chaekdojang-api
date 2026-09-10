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
        /** 지금 응모를 받는 중인지 */
        boolean acceptingEntries,
        /** 접수 마감 시각이 지났는지. 주최자가 마감 처리를 미뤄도 화면에 마감으로 보여주기 위해 쓴다. */
        boolean submitClosed,
        long entryCount,
        List<ContestBookResponse> books
) {
    public static ContestSummaryResponse of(Contest contest, long entryCount,
                                            List<ContestBookResponse> books, LocalDateTime now) {
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
                contest.isAcceptingEntries(now),
                now.isAfter(contest.getSubmitEndAt()),
                entryCount,
                books
        );
    }
}
