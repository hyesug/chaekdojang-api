package com.chaekdojang.api.domain.library.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryResponseTest {

    @Test
    void bookInfoIncludesInferredGenreForExistingKakaoBook() {
        Book book = Book.builder()
                .title("체호프 단편선")
                .author("안톤 체호프")
                .source(BookSource.KAKAO)
                .description("다양한 인물 군상을 담은 단편소설 모음이다.")
                .build();

        LibraryResponse.BookInfo response = LibraryResponse.BookInfo.from(book);

        assertThat(response.category()).isEqualTo("소설");
    }
}
