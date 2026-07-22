package com.chaekdojang.api.infra.naver;

import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void matchesTitleFragmentsButRejectsTruncatedCandidate() {
        assertThat(client.titleMatches("웨딩케이크", "웨딩케이크")).isTrue();
        assertThat(client.titleMatches("웨딩케이크", "웨딩케이크 외전")).isTrue();
        assertThat(client.titleMatches("용꿈", "은행원도 용꿈을 꾸나요 - 판타지 웹소설")).isTrue();
        assertThat(client.titleMatches("착한오빠, 나쁜오빠", "착한 오빠")).isFalse();
    }

    @Test
    void acceptsNaverWebNovelListsButRejectsEpisodeAndNoticePages() {
        assertThat(client.isSearchResultUrl(
                WebNovelPlatform.NAVER_SERIES,
                "https://novel.naver.com/best/list?novelId=1159312"
        )).isTrue();
        assertThat(client.isSearchResultUrl(
                WebNovelPlatform.NAVER_SERIES,
                "https://novel.naver.com/best/detail?novelId=1145814&volumeNo=242"
        )).isFalse();
    }

    @Test
    void parsesTitleAuthorAndWorkListFromOfficialNaverWebNovelSearch() {
        String html = """
                <li class="item">
                  <a href="/best/list?novelId=1159312" class="link">
                    <span class="title"><strong>착한 오빠, 나쁜 오빠</strong></span>
                    <span class="author">봉자까</span>
                    <span class="tag">#로맨스 #동거</span>
                  </a>
                </li>
                """;

        List<WebNovelSearchResult> results = client.parseNaverWebNovelSearch("착한오빠, 나쁜오빠", html);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.title()).isEqualTo("착한 오빠, 나쁜 오빠");
            assertThat(result.author()).isEqualTo("봉자까");
            assertThat(result.sourceUrl()).isEqualTo("https://novel.naver.com/best/list?novelId=1159312");
            assertThat(result.externalId()).isEqualTo("webnovel-1159312");
        });
    }

    @Test
    void extractsSpacedWorkTitleMentionedInSearchResult() {
        assertThat(client.extractMentionedWorkTitle(
                "착한오빠, 나쁜오빠",
                "&lt;<b>착한 오빠, 나쁜 오빠</b>&gt; 연재 시작했습니다 :), 순한맛인 줄 알았더니 매운맛"
        )).isEqualTo("착한 오빠, 나쁜 오빠");

        assertThat(client.extractMentionedWorkTitle(
                "웨딩케이크",
                "<b>웨딩 케이크</b> : 네이버웹소설"
        )).isEqualTo("웨딩 케이크");
    }
}
