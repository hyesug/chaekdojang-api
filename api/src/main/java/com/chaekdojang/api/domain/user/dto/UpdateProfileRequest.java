package com.chaekdojang.api.domain.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        String nickname,
        @Size(max = 150)
        String bio,
        String profileImage
) {}
