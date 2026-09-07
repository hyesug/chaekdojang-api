package com.chaekdojang.api.domain.dojangdan.dto;

import jakarta.validation.constraints.Size;

public record CampaignApplyRequest(
        @Size(max = 2000) String message
) {
}
