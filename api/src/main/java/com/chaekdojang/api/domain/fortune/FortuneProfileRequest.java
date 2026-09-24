package com.chaekdojang.api.domain.fortune;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 날짜는 화면에서 양력으로 바꾼 뒤 보낸다. 시각을 모르면 hour 를 비운다 */
public record FortuneProfileRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Pattern(regexp = "male|female") String gender,
        @NotNull @Min(1900) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(1) @Max(31) Integer day,
        @Min(0) @Max(23) Integer hour,
        @Min(0) @Max(59) Integer minute,
        @NotBlank @Size(max = 40) String birthPlace,
        @NotBlank @Size(max = 40) String homePlace,
        boolean dst
) {}
