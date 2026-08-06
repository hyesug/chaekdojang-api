package com.chaekdojang.api.domain.recap.dto;

import com.chaekdojang.api.domain.recap.ReadingRecapFrequency;
import jakarta.validation.constraints.NotNull;

public record ReadingRecapPreferenceRequest(
        @NotNull ReadingRecapFrequency frequency,
        Boolean includeFollowing,
        Boolean includeMemories
) {
    public boolean shouldIncludeFollowing() { return !Boolean.FALSE.equals(includeFollowing); }
    public boolean shouldIncludeMemories() { return !Boolean.FALSE.equals(includeMemories); }
}
