package com.chaekdojang.api.domain.book.dto;

import java.util.List;

/** 주제 하나와 그 주제의 추천 책 — 운세 리포트가 "왜 이 책인지"를 주제별로 붙인다 */
public record BookThemeRecommendationResponse(
        String theme,
        String label,
        List<BookResponse> books
) {}
