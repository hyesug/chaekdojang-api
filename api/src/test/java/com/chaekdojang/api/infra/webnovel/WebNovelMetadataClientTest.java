package com.chaekdojang.api.infra.webnovel;

import com.chaekdojang.api.domain.book.BookSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebNovelMetadataClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebNovelMetadataClient client = new WebNovelMetadataClient(objectMapper);

    @Test
    void prefersPortraitNaverSeriesCoverOverOpenGraphPreview() {
        String html = """
                <meta property="og:image" content="https://comicthumb-phinf.pstatic.net/preview.jpg?type=m600x314">
                <a class="pic_area">
                  <img src="https://comicthumb-phinf.pstatic.net/cover.jpg?type=m260">
                </a>
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(
                BookSource.NAVER_SERIES, html);

        assertThat(metadata.thumbnail())
                .isEqualTo("https://comicthumb-phinf.pstatic.net/cover.jpg?type=m260");
    }

    @Test
    void parsesNaverWebNovelPortraitCover() {
        String html = """
                <div class="section_area_info">
                  <span class="thumbnail">
                    <img src="https://novel-phinf.pstatic.net/cover.jpg?type=f200_276_2">
                  </span>
                </div>
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(
                BookSource.NAVER_SERIES, html);

        assertThat(metadata.thumbnail())
                .isEqualTo("https://novel-phinf.pstatic.net/cover.jpg?type=f200_276_2");
    }

    @Test
    void parsesRidiAuthorAndCoverFromOfficialMetadata() {
        String html = """
                <meta property="og:image" content="https://img.ridicdn.net/cover/6188000157/xxlarge#1">
                <meta property="og:description" content="한 번뿐인 삶을 다시 시작한 주인공의 이야기">
                <script type="application/ld+json">
                  {
                    "@type": "Book",
                    "description": "<p>은행원으로 살던 주인공이<br>새로운 운명을 만난다.</p>",
                    "author": [
                      { "name": "연산호" },
                      { "name": "P" },
                      { "name": "연산호" }
                    ]
                  }
                </script>
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(BookSource.RIDI, html);

        assertThat(metadata.author()).isEqualTo("연산호, P");
        assertThat(metadata.thumbnail())
                .isEqualTo("https://img.ridicdn.net/cover/6188000157/xxlarge");
        assertThat(metadata.description()).isEqualTo("은행원으로 살던 주인공이 새로운 운명을 만난다.");
    }

    @Test
    void buildsKakaoCoverUrlFromPublicContentMetadata() throws Exception {
        String json = """
                {
                  "result": {
                    "content": {
                      "authors": "백덕수",
                      "thumbnail": "Du5oG/hzVqEy9QjX/lN2aZRgLz8pegvqODVjfE1",
                      "description": "4년차 공시생, 낯선 몸에 빙의해 3년 전으로 돌아왔다."
                    }
                  }
                }
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseKakaoMetadata(
                objectMapper.readTree(json));

        assertThat(metadata.author()).isEqualTo("백덕수");
        assertThat(metadata.thumbnail()).isEqualTo(
                "https://page-images.kakaoentcdn.com/download/resource?kid="
                        + "Du5oG/hzVqEy9QjX/lN2aZRgLz8pegvqODVjfE1"
        );
        assertThat(metadata.description()).isEqualTo("4년차 공시생, 낯선 몸에 빙의해 3년 전으로 돌아왔다.");
    }

    @Test
    void parsesDescriptionFromOpenGraphMetadata() {
        String html = """
                <meta property="og:description" content="몰락한 가문을 다시 일으키는 주인공의 성장 이야기">
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(BookSource.MUNPIA, html);

        assertThat(metadata.description()).isEqualTo("몰락한 가문을 다시 일으키는 주인공의 성장 이야기");
    }

    @Test
    void keepsCompleteOfficialDescriptionLongerThanTwoThousandCharacters() {
        String description = "작품 소개 ".repeat(500);
        String html = "<meta property=\"og:description\" content=\"" + description + "\">";

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(BookSource.MUNPIA, html);

        assertThat(metadata.description()).isEqualTo(description.trim());
        assertThat(metadata.description()).hasSizeGreaterThan(2000);
    }

    @Test
    void ignoresImageFromUntrustedHost() {
        String html = """
                <meta property="og:image" content="https://example.com/not-a-ridi-cover.jpg">
                """;

        WebNovelMetadataClient.Metadata metadata = client.parseHtmlMetadata(BookSource.RIDI, html);

        assertThat(metadata.thumbnail()).isEmpty();
    }
}
