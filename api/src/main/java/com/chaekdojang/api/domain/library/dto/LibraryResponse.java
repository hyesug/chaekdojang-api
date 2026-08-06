package com.chaekdojang.api.domain.library.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookGenreClassifier;
import com.chaekdojang.api.domain.library.Library;
import com.chaekdojang.api.domain.library.LibraryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LibraryResponse(
        Long id,
        BookInfo book,
        LibraryStatus status,
        LocalDate completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record BookInfo(
            Long id,
            String isbn13,
            String title,
            String author,
            String thumbnail,
            String category
    ) {
        public static BookInfo from(Book book) {
            return from(book, BookGenreClassifier.resolve(book));
        }

        public static BookInfo from(Book book, String category) {
            return new BookInfo(book.getId(), book.getIsbn13(), book.getTitle(),
                    book.getAuthor(), book.getThumbnail(), category);
        }
    }

    public static LibraryResponse from(Library library) {
        return from(library, null);
    }

    public static LibraryResponse from(Library library, String category) {
        return new LibraryResponse(
                library.getId(),
                BookInfo.from(library.getBook(), category),
                library.getStatus(),
                library.getCompletedAt(),
                library.getCreatedAt(),
                library.getUpdatedAt()
        );
    }

    public static LibraryResponse fromPublicReviewBook(Book book) {
        return fromPublicReviewBook(book, null);
    }

    public static LibraryResponse fromPublicReviewBook(Book book, String category) {
        return new LibraryResponse(
                null,
                BookInfo.from(book, category),
                LibraryStatus.FINISHED,
                null,
                null,
                null
        );
    }
}
