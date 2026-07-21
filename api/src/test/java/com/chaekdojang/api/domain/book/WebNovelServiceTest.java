package com.chaekdojang.api.domain.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.WebNovelRegisterRequest;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.infra.kakao.KakaoWebNovelClient;
import com.chaekdojang.api.infra.naver.NaverWebNovelClient;
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
    private KakaoWebNovelClient kakaoWebNovelClient;
    private NaverWebNovelClient naverWebNovelClient;
    private WebNovelService webNovelService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        kakaoWebNovelClient = mock(KakaoWebNovelClient.class);
        naverWebNovelClient = mock(NaverWebNovelClient.class);
        webNovelService = new WebNovelService(
                bookRepository,
                reviewRepository,
                kakaoWebNovelClient,
                naverWebNovelClient,
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
