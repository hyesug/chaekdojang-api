package com.chaekdojang.api.domain.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.WebNovelRegisterRequest;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.infra.kakao.KakaoWebNovelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    private WebNovelService webNovelService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        webNovelService = new WebNovelService(
                bookRepository,
                reviewRepository,
                mock(KakaoWebNovelClient.class),
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
}
