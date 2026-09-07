package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignStatusUpdateRequest(
        @NotNull CampaignStatus status
) {
}
