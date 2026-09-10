package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.ContestEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ContestCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        @Size(max = 2000) String prizeDescription,
        @NotNull ContestEntryType entryType,
        /** 비우면 자유주제 공모전 */
        List<Long> bookIds,
        @NotNull LocalDateTime submitStartAt,
        @NotNull LocalDateTime submitEndAt,
        @NotNull LocalDateTime announceAt
) {
}
