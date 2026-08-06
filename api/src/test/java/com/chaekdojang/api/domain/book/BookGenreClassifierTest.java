package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookGenreClassifierTest {

    @Test
    void prioritizesExplicitBookTypeOverExternalTopicCategory() {
        assertThat(BookGenreClassifier.resolve(
                "History", BookSource.GOOGLE_BOOKS, "어떤 소설", "장편소설 소개"))
                .isEqualTo("소설");
    }

    @Test
    void keepsExternalCategoryWhenBookTypeIsNotExplicit() {
        assertThat(BookGenreClassifier.resolve(
                "History", BookSource.GOOGLE_BOOKS, "세계의 흐름", "시대의 변화를 살펴본다"))
                .isEqualTo("역사/문화");
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
                .isEqualTo("경제/경영");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "이기적 유전자", "리처드 도킨스의 책"))
                .isEqualTo("과학");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "거꾸로 읽는 세계사", "새롭게 쓴 세계사"))
                .isEqualTo("역사/문화");
    }

    @Test
    void usesWebNovelAndFallbackCategories() {
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.NAVER_SERIES, "작품", null)).isEqualTo("소설");
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "분류 근거 없는 책", null)).isNull();
    }

    @Test
    void mapsExternalAndLegacyValuesToRequestedCategories() {
        assertThat(BookGenreClassifier.resolve(
                "Health & Fitness", BookSource.GOOGLE_BOOKS, "건강책", null)).isEqualTo("건강");
        assertThat(BookGenreClassifier.resolve(
                "사회·정치", BookSource.KAKAO, "사회책", null)).isEqualTo("정치/사회");
        assertThat(BookGenreClassifier.resolve(
                "Computers", BookSource.GOOGLE_BOOKS, "개발책", null)).isEqualTo("컴퓨터/IT");
        assertThat(BookGenreClassifier.resolve(
                "Young Adult Fiction", BookSource.GOOGLE_BOOKS, "청소년 소설", null)).isEqualTo("청소년");
        assertThat(BookGenreClassifier.resolve(
                "Juvenile Fiction", BookSource.GOOGLE_BOOKS, "어린이책", null)).isEqualTo("어린이(초등)");
    }

    @Test
    void reclassifiesLegacyKakaoCategoryFromCurrentBookText() {
        assertThat(BookGenreClassifier.resolve(
                "역사", BookSource.KAKAO, "체호프 단편선", "러시아 문학을 대표하는 단편소설 모음"))
                .isEqualTo("소설");
    }

    @Test
    void prioritizesBookTypeOverIncidentalTopicWords() {
        assertThat(BookGenreClassifier.resolve(
                "과학", BookSource.KAKAO, "불안(리커버:K)", "알랭 드 보통",
                "알랭 드 보통의 인문철학 에세이. 천문학자 등 유명인이 추천했다."))
                .isEqualTo("인문");
        assertThat(BookGenreClassifier.resolve(
                "예술/대중문화", BookSource.KAKAO, "프로젝트 헤일메리", "앤디 위어",
                "전 세계 SF 팬들을 사로잡은 화제의 소설이 영화로 향한다."))
                .isEqualTo("소설");
    }

    @Test
    void classifiesDemianWithoutExternalDescription() {
        assertThat(BookGenreClassifier.resolve(
                null, BookSource.KAKAO, "데미안", "헤르만 헤세",
                "데미안은 헤르만 헤세의 책입니다."))
                .isEqualTo("소설");
    }
}
