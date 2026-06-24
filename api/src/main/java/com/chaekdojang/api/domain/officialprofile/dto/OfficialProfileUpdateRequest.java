package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfficialProfileUpdateRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 2000) String bio,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String officialUrl,
        @Size(max = 500) String instagramUrl,
        @Size(max = 500) String brunchUrl,
        @Size(max = 500) String tumblbugUrl,
        @Size(max = 255) String contactEmail,
        @NotNull OfficialProfileStatus status,
        boolean verified,
        boolean featured
) {
}
