package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupJoinPolicy;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingGroupCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String imageUrl,
        ReadingGroupVisibility visibility,
        ReadingGroupJoinPolicy joinPolicy
) {
}
