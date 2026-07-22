package com.chaekdojang.api.domain.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.WebNovelRegisterRequest;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.infra.kakao.KakaoWebNovelClient;
import com.chaekdojang.api.infra.naver.NaverWebNovelClient;
import com.chaekdojang.api.infra.ridi.RidiWebNovelClient;
import com.chaekdojang.api.infra.webnovel.WebNovelMetadataClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebNovelServiceTest {

    private BookRepository bookRepository;
    private ReviewRepository reviewRepository;
    private RidiWebNovelClient ridiWebNovelClient;
    private KakaoWebNovelClient kakaoWebNovelClient;
    private NaverWebNovelClient naverWebNovelClient;
    private WebNovelMetadataClient webNovelMetadataClient;
    private WebNovelService webNovelService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        ridiWebNovelClient = mock(RidiWebNovelClient.class);
        kakaoWebNovelClient = mock(KakaoWebNovelClient.class);
        naverWebNovelClient = mock(NaverWebNovelClient.class);
        webNovelMetadataClient = mock(WebNovelMetadataClient.class);
        when(webNovelMetadataClient.findMetadata(any(), any()))
                .thenReturn(WebNovelMetadataClient.Metadata.empty());
        webNovelService = new WebNovelService(
                bookRepository,
                reviewRepository,
                ridiWebNovelClient,
                kakaoWebNovelClient,
                naverWebNovelClient,
                webNovelMetadataClient,
                mock(StringRedisTemplate.class),
                new ObjectMapper()
        );
    }

    @Test
    void registersOfficialWebNovelUrlWithCanonicalIdentity() {
        when(bookRepository.findBySourceAndExternalId(BookSource.RIDI, "4362000001"))
                .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(nullable(Long.class))).thenReturn(0L);
        when(webNovelMetadataClient.findMetadata(BookSource.RIDI, "https://ridibooks.com/books/4362000001"))
                .thenReturn(new WebNovelMetadataClient.Metadata(
                        "김수지",
                        "https://img.ridicdn.net/cover/4362000001/large"
                ));

        BookResponse response = webNovelService.register(new WebNovelRegisterRequest(
                "상수리나무 아래",
                "김수지",
                BookSource.RIDI,
                "https://ridibooks.com/books/4362000001?_s=search"
        ));

        assertThat(response.title()).isEqualTo("상수리나무 아래");
        assertThat(response.author()).isEqualTo("김수지");
        assertThat(response.contentType()).isEqualTo("WEB_NOVEL");
        assertThat(response.externalId()).isEqualTo("4362000001");
        assertThat(response.sourceUrl()).isEqualTo("https://ridibooks.com/books/4362000001");
        assertThat(response.thumbnail()).isEqualTo("https://img.ridicdn.net/cover/4362000001/large");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void registersNaverWebNovelUrlWithWebNovelIdentity() {
        when(bookRepository.findBySourceAndExternalId(BookSource.NAVER_SERIES, "webnovel-1159312"))
                .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(nullable(Long.class))).thenReturn(0L);

        BookResponse response = webNovelService.register(new WebNovelRegisterRequest(
                "착한 오빠, 나쁜 오빠",
                "봉자까",
                BookSource.NAVER_SERIES,
                "https://novel.naver.com/best/list?OSType=pc&novelId=1159312&page=7"
        ));

        assertThat(response.publisher()).isEqualTo("네이버 웹소설");
        assertThat(response.externalId()).isEqualTo("webnovel-1159312");
        assertThat(response.sourceUrl())
                .isEqualTo("https://novel.naver.com/best/list?novelId=1159312");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void rejectsUrlThatDoesNotMatchPlatform() {
        assertThatThrownBy(() -> webNovelService.register(new WebNovelRegisterRequest(
                "상수리나무 아래",
                "김수지",
                BookSource.RIDI,
                "https://page.kakao.com/content/56566288"
        ))).isInstanceOf(CustomException.class);
    }

    @Test
    void mergesNaverAndKakaoResultsWithoutDuplicates() {
        WebNovelSearchResult naver = result("재혼 황후", BookSource.NAVER_SERIES, "3713078");
        WebNovelSearchResult kakaoDuplicate = result("재혼 황후", BookSource.NAVER_SERIES, "3713078");
        WebNovelSearchResult kakaoOnly = result("상수리나무 아래", BookSource.RIDI, "4362000001");
        when(naverWebNovelClient.search("재혼 황후")).thenReturn(List.of(naver));
        when(kakaoWebNovelClient.search("재혼 황후")).thenReturn(List.of(kakaoDuplicate, kakaoOnly));

        List<WebNovelSearchResult> results = webNovelService.search("재혼 황후");

        assertThat(results).containsExactly(naver, kakaoOnly);
    }

    @Test
    void putsExactTitleBeforeRelatedWorks() {
        WebNovelSearchResult prologue = result(
                "데뷔 못 하면 죽는 병 걸림 시즌3 프롤로그",
                BookSource.KAKAO_PAGE,
                "59782511"
        );
        WebNovelSearchResult original = result(
                "데뷔 못 하면 죽는 병 걸림",
                BookSource.KAKAO_PAGE,
                "56325530"
        );
        when(naverWebNovelClient.search("데뷔 못 하면 죽는 병 걸림")).thenReturn(List.of(prologue, original));
        when(kakaoWebNovelClient.search("데뷔 못 하면 죽는 병 걸림")).thenReturn(List.of());

        List<WebNovelSearchResult> results = webNovelService.search("데뷔 못 하면 죽는 병 걸림");

        assertThat(results).containsExactly(original, prologue);
    }

    @Test
    void enrichesMissingRidiAuthorFromOfficialWorkPage() {
        WebNovelSearchResult ridi = result("웨딩케이크 살인사건", BookSource.RIDI, "2089000054");
        when(naverWebNovelClient.search("웨딩케이크")).thenReturn(List.of(ridi));
        when(kakaoWebNovelClient.search("웨딩케이크")).thenReturn(List.of());
        when(webNovelMetadataClient.findMetadata(BookSource.RIDI, ridi.sourceUrl()))
                .thenReturn(new WebNovelMetadataClient.Metadata(
                        "조앤 플루크",
                        "https://img.ridicdn.net/cover/2089000054/large"
                ));

        List<WebNovelSearchResult> results = webNovelService.search("웨딩케이크");

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.author()).isEqualTo("조앤 플루크");
            assertThat(result.thumbnail()).isEqualTo("https://img.ridicdn.net/cover/2089000054/large");
        });
    }

    @Test
    void deduplicatesRidiVolumesWithMarketingTitleSuffix() {
        WebNovelSearchResult firstVolume = result("은행원도 용꿈을 꾸나요", BookSource.RIDI, "6188000001");
        WebNovelSearchResult laterVolume = result("은행원도 용꿈을 꾸나요 - 판타지 웹소설", BookSource.RIDI, "6188000155");
        when(naverWebNovelClient.search("은행원도 용꿈을 꾸나요")).thenReturn(List.of(firstVolume, laterVolume));
        when(kakaoWebNovelClient.search("은행원도 용꿈을 꾸나요")).thenReturn(List.of());
        when(webNovelMetadataClient.findMetadata(any(), any()))
                .thenReturn(new WebNovelMetadataClient.Metadata(
                        "연산호",
                        "https://img.ridicdn.net/cover/6188000001/large"
                ));

        List<WebNovelSearchResult> results = webNovelService.search("은행원도 용꿈을 꾸나요");

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.externalId()).isEqualTo("6188000001");
            assertThat(result.author()).isEqualTo("연산호");
        });
    }

    private WebNovelSearchResult result(String title, BookSource platform, String externalId) {
        return new WebNovelSearchResult(
                title,
                "",
                platform,
                platform.name(),
                "https://example.com/" + externalId,
                externalId,
                ""
        );
    }
}
