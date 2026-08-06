package com.chaekdojang.api.domain.recap.dto;

import com.chaekdojang.api.domain.recap.ReadingRecapFrequency;
import com.chaekdojang.api.domain.recap.ReadingRecapPreference;

import java.time.LocalDateTime;

public record ReadingRecapPreferenceResponse(
        boolean enabled,
        ReadingRecapFrequency frequency,
        boolean includeFollowing,
        boolean includeMemories,
        LocalDateTime lastDeliveredAt
) {
    public static ReadingRecapPreferenceResponse from(ReadingRecapPreference value) {
        return new ReadingRecapPreferenceResponse(
                value.isEnabled(), value.getFrequency(), value.isIncludeFollowing(),
                value.isIncludeMemories(), value.getLastDeliveredAt());
    }
}
