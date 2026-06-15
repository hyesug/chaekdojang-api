package com.chaekdojang.api.domain.book.dto;

import com.chaekdojang.api.domain.book.Book;

public record BookResponse(
        Long id,
        String isbn13,
        String title,
        String author,
        String publisher,
        String thumbnail,
        String source,
        String category,
        long reviewCount
) {
    public static BookResponse from(Book book) {
        return from(book, 0L);
    }

    public static BookResponse from(Book book, long reviewCount) {
        return new BookResponse(
                book.getId(),
                book.getIsbn13(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getThumbnail(),
                book.getSource().name(),
                book.getCategory(),
                reviewCount
        );
    }
}
