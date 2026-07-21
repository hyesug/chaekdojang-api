package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebNovelPlatformTest {

    @Test
    void resolvesNaverSeriesWork() {
        WebNovelPlatform.ResolvedWork work = WebNovelPlatform.NAVER_SERIES
                .resolve("https://m.series.naver.com/novel/detail.series?sortOrder=DESC&productNo=3400123")
                .orElseThrow();

        assertThat(work.externalId()).isEqualTo("3400123");
        assertThat(work.canonicalUrl())
                .isEqualTo("https://series.naver.com/novel/detail.series?productNo=3400123");
    }

    @Test
    void resolvesKakaoPageViewerAsWork() {
        WebNovelPlatform.ResolvedWork work = WebNovelPlatform.KAKAO_PAGE
                .resolve("https://page.kakao.com/content/56566288/viewer/62872637")
                .orElseThrow();

        assertThat(work.externalId()).isEqualTo("56566288");
        assertThat(work.canonicalUrl()).isEqualTo("https://page.kakao.com/content/56566288");
    }

    @Test
    void resolvesRidiWorkWithoutTrackingQuery() {
        WebNovelPlatform.ResolvedWork work = WebNovelPlatform.RIDI
                .resolve("https://ridibooks.com/books/4362000001?_s=search&_q=test")
                .orElseThrow();

        assertThat(work.externalId()).isEqualTo("4362000001");
        assertThat(work.canonicalUrl()).isEqualTo("https://ridibooks.com/books/4362000001");
    }

    @Test
    void resolvesMunpiaMobileWork() {
        WebNovelPlatform.ResolvedWork work = WebNovelPlatform.MUNPIA
                .resolve("https://m.munpia.com/novel/detail/578008")
                .orElseThrow();

        assertThat(work.externalId()).isEqualTo("578008");
        assertThat(work.canonicalUrl()).isEqualTo("https://novel.munpia.com/578008");
    }

    @Test
    void rejectsWrongPlatformAndNonWorkUrls() {
        assertThat(WebNovelPlatform.RIDI.resolve("https://page.kakao.com/content/56566288")).isEmpty();
        assertThat(WebNovelPlatform.NAVER_SERIES.resolve("https://series.naver.com/comic/detail.series?productNo=5133669")).isEmpty();
        assertThat(WebNovelPlatform.MUNPIA.resolve("https://example.com/578008")).isEmpty();
    }
}
