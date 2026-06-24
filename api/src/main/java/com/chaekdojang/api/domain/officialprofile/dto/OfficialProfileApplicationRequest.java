package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfficialProfileApplicationRequest(
        @NotNull OfficialProfileType type,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 2000) String bio,
        @Size(max = 500) String officialUrl,
        @NotBlank @Email @Size(max = 255) String contactEmail,
        @Size(max = 500) String proofUrl
) {
}
