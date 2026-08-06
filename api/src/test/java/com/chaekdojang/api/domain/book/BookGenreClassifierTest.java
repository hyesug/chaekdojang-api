package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookGenreClassifierTest {

    @Test
    void keepsExternalCategoryBeforeInferring() {
        assertThat(BookGenreClassifier.resolve(
                "History", BookSource.GOOGLE_BOOKS, "어떤 소설", "장편소설 소개"))
                .isEqualTo("역사");
    }

    @Test
    void infersNovelFromKakaoDescription() {
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "급류", "두 인물의 사랑을 그린 두 번째 장편소설"))
                .isEqualTo("소설");
    }

    @Test
    void infersBroadNonFictionGenres() {
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "돈의 속성", "경제경영 필독서"))
                .isEqualTo("경제·경영");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "이기적 유전자", "리처드 도킨스의 책"))
                .isEqualTo("과학");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "거꾸로 읽는 세계사", "새롭게 쓴 세계사"))
                .isEqualTo("역사");
    }

    @Test
    void usesWebNovelAndFallbackCategories() {
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.NAVER_SERIES, "작품", null)).isEqualTo("웹소설");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "분류 근거 없는 책", null)).isEqualTo("기타");
    }
}
