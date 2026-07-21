package com.chaekdojang.api.domain.book;

public enum BookSource {
    KAKAO,
    GOOGLE_BOOKS,
    NAVER_SERIES,
    KAKAO_PAGE,
    RIDI,
    MUNPIA;

    public boolean isWebNovel() {
        return this == NAVER_SERIES || this == KAKAO_PAGE || this == RIDI || this == MUNPIA;
    }
}
