package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.readinggroup.ReadingGroupBook;

import java.time.LocalDateTime;

public record ReadingGroupBookResponse(
        Long id,
        Long bookId,
        String title,
        String author,
        String publisher,
        String thumbnail,
        String slug,
        String note,
        long reviewCount,
        LocalDateTime createdAt
) {
    public static ReadingGroupBookResponse of(ReadingGroupBook groupBook, long reviewCount) {
        Book book = groupBook.getBook();
        return new ReadingGroupBookResponse(
                groupBook.getId(),
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getThumbnail(),
                book.getSlug(),
                groupBook.getNote(),
                reviewCount,
                groupBook.getCreatedAt()
        );
    }
}
