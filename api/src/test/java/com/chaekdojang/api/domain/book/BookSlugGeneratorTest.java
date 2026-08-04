package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookSlugGeneratorTest {

    @Test
    @DisplayName("숫자로만 구성된 책 slug에는 book 접두사를 붙인다")
    void create_numericOnlySlug_addsBookPrefix() {
        String slug = BookSlugGenerator.create(
                "6인의 용의자",
                "비카스 스와루프",
                "9788954608145",
                529L
        );

        assertThat(slug).isEqualTo("book-6");
    }

    @Test
    @DisplayName("알려진 책의 영문 slug는 그대로 유지한다")
    void create_knownBook_keepsKnownSlug() {
        assertThat(BookSlugGenerator.create("데미안", "헤르만 헤세", null, 1L))
                .isEqualTo("demian");
    }
}
