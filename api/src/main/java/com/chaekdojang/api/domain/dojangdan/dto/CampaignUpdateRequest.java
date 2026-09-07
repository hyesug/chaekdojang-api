package com.chaekdojang.api.domain.dojangdan.dto;

import com.chaekdojang.api.domain.dojangdan.CampaignDeliveryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CampaignUpdateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 5000) String description,
        @Min(1) int recruitCount,
        @NotNull LocalDateTime recruitStartAt,
        @NotNull LocalDateTime recruitEndAt,
        @NotNull LocalDateTime reviewDueAt,

        /** 공개 모집 전 관심 독자에게만 여는 시간(0~48). 비우면 기존 값을 유지한다. */
        @Min(0) @Max(48) Integer priorityInviteHours,

        /** 배본 방식. 비우면 기존 값을 유지한다. */
        CampaignDeliveryType deliveryType,

        /** 전자책 열람 만료 = 독후감 마감 + 이 일수(0~90). 비우면 기존 값을 유지한다. */
        @Min(0) @Max(90) Integer ebookAccessExtraDays
) {
}
