package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 관리자가 신청 절차 없이 공식 프로필을 직접 만들 때 쓴다. */
public record OfficialProfileCreateRequest(
        @NotNull OfficialProfileType type,
        @NotBlank @Size(max = 100) String displayName,
        String bio,
        @Size(max = 500) String officialUrl,
        @Size(max = 255) String contactEmail
) {
}
