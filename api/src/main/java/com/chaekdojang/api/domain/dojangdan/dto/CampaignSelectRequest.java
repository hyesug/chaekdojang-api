package com.chaekdojang.api.domain.dojangdan.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CampaignSelectRequest(
        @NotEmpty List<Long> applicationIds,
        boolean rejectOthers // true면 선정되지 않은 나머지 신청을 모두 미선정 처리한다
) {
}
