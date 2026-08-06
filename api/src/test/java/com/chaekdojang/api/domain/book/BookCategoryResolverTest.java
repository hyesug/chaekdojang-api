package com.chaekdojang.api.domain.book;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookCategoryResolverTest {

    @Test
    void sharesStrongCategoryEvidenceAcrossEditionsOfSameWork() {
        Book target = book("어떤 고전", "어떤 작가", BookSource.KAKAO, null, null);
        Book narrativeEdition = book(
                "어떤 고전(리커버 특별판)", "어떤작가", BookSource.GOOGLE_BOOKS, "인문",
                "평범한 소년이 두려움을 품고 살아간다. 그는 그러던 중 뜻밖의 사건을 만난다.");
        Book fictionEdition = book(
                "어떤 고전[초판본 표지]", "어떤 작가", BookSource.GOOGLE_BOOKS, "Fiction", null);
        BookRepository repository = mock(BookRepository.class);
        when(repository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc())
                .thenReturn(List.of(target, narrativeEdition, fictionEdition));

        BookCategoryResolver resolver = new BookCategoryResolver(repository);

        assertThat(resolver.resolve(target)).isEqualTo("소설");
    }

    @Test
    void choosesWorkConsensusOverOneInconsistentEditionCategory() {
        Book target = book("사색의 기록", "어떤 철학자", BookSource.GOOGLE_BOOKS, "Fiction", null);
        Book first = book(
                "사색의 기록(개정판)", "어떤 철학자", BookSource.KAKAO, "시/에세이",
                "스토아 철인의 사상과 이성, 인간 본성에 대한 성찰을 담은 에세이");
        Book second = book(
                "사색의 기록[특별판]", "어떤철학자", BookSource.GOOGLE_BOOKS, "Philosophy", null);
        BookRepository repository = mock(BookRepository.class);
        when(repository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc())
                .thenReturn(List.of(target, first, second));

        BookCategoryResolver resolver = new BookCategoryResolver(repository);

        assertThat(resolver.resolve(target)).isEqualTo("인문");
    }

    @Test
    void sharesVerifiedCategoryAcrossFuzzyAuthorSpellings() {
        Book target = book("숲의 기록", "헨리 데이비드 소로", BookSource.GOOGLE_BOOKS, "Art", null);
        Book verifiedEdition = book("숲의 기록(특별판)", "헨리데이빗소로우", BookSource.KAKAO, null, null);
        verifiedEdition.updateVerifiedCategory("시/에세이");
        BookRepository repository = mock(BookRepository.class);
        when(repository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc())
                .thenReturn(List.of(target, verifiedEdition));

        BookCategoryResolver resolver = new BookCategoryResolver(repository);

        assertThat(resolver.resolve(target)).isEqualTo("시/에세이");
    }

    @Test
    void doesNotMultiplyDuplicateExternalEditionErrors() {
        Book target = book("사유의 숲", "어떤 작가", BookSource.KAKAO, null,
                "삶을 돌아보는 철학서");
        Book wrongFirst = book("사유의 숲(특별판)", "어떤작가", BookSource.GOOGLE_BOOKS, "Art", null);
        Book wrongSecond = book("사유의 숲(양장본)", "어떤 작가", BookSource.GOOGLE_BOOKS, "Art", null);
        BookRepository repository = mock(BookRepository.class);
        when(repository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc())
                .thenReturn(List.of(target, wrongFirst, wrongSecond));

        BookCategoryResolver resolver = new BookCategoryResolver(repository);

        assertThat(resolver.resolve(target)).isEqualTo("인문");
    }

    private Book book(String title, String author, BookSource source, String category, String description) {
        return Book.builder()
                .title(title)
                .author(author)
                .source(source)
                .category(category)
                .description(description)
                .build();
    }
}
