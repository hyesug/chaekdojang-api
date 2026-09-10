package com.chaekdojang.api.domain.contest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 수상자 지정. 목록에 없는 응모작은 수상 지정이 해제된다. */
public record ContestAwardSaveRequest(
        @NotEmpty @Valid List<AwardItem> awards
) {
    public record AwardItem(
            @NotNull Long entryId,
            @NotNull @Min(1) Integer awardRank,
            @NotBlank @Size(max = 50) String awardName
    ) {
    }
}
