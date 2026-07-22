package com.chaekdojang.api.infra.naver;

import com.chaekdojang.api.domain.book.WebNovelPlatform;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverWebNovelClientTest {

    private final NaverWebNovelClient client = new NaverWebNovelClient("", "");

    @Test
    void expandsNaverSeriesSearchWithExactAndServiceQueries() {
        assertThat(client.searchQueries("착한오빠, 나쁜오빠", WebNovelPlatform.NAVER_SERIES))
                .containsExactly(
                        "착한오빠, 나쁜오빠 site:series.naver.com/novel",
                        "\"착한오빠, 나쁜오빠\" site:series.naver.com/novel",
                        "착한오빠, 나쁜오빠 네이버 시리즈 site:series.naver.com/novel",
                        "착한오빠, 나쁜오빠 site:novel.naver.com",
                        "\"착한오빠, 나쁜오빠\" site:novel.naver.com"
                );
    }

    @Test
    void keepsExactAndRelatedTitlesButRejectsShortPartialTitle() {
        assertThat(client.titleMatches("웨딩케이크", "웨딩케이크")).isTrue();
        assertThat(client.titleMatches("웨딩케이크", "웨딩케이크 외전")).isTrue();
        assertThat(client.titleMatches("착한오빠, 나쁜오빠", "착한 오빠")).isFalse();
    }
}
