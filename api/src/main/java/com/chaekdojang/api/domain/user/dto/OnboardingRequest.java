package com.chaekdojang.api.domain.user.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingRequest(
        @Size(max = 10) List<@Size(max = 30) String> genres
) {
}
