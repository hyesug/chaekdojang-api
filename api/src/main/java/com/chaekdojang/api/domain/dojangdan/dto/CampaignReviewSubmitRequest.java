package com.chaekdojang.api.domain.dojangdan.dto;

import jakarta.validation.constraints.NotNull;

public record CampaignReviewSubmitRequest(
        @NotNull Long reviewId
) {
}
