package com.chaekdojang.api.domain.library.dto;

import com.chaekdojang.api.domain.book.Book;
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
    public record BookInfo(Long id, String isbn13, String title, String author, String thumbnail) {
        public static BookInfo from(Book book) {
            return new BookInfo(book.getId(), book.getIsbn13(), book.getTitle(),
                    book.getAuthor(), book.getThumbnail());
        }
    }

    public static LibraryResponse from(Library library) {
        return new LibraryResponse(
                library.getId(),
                BookInfo.from(library.getBook()),
                library.getStatus(),
                library.getCompletedAt(),
                library.getCreatedAt(),
                library.getUpdatedAt()
        );
    }
}
