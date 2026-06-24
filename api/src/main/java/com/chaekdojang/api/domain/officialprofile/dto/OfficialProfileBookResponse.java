package com.chaekdojang.api.domain.officialprofile.dto;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileBook;

public record OfficialProfileBookResponse(
        Long id,
        String isbn13,
        String title,
        String author,
        String publisher,
        String thumbnail,
        long reviewCount
) {
    public static OfficialProfileBookResponse of(OfficialProfileBook profileBook, long reviewCount) {
        Book book = profileBook.getBook();
        return new OfficialProfileBookResponse(
                book.getId(),
                book.getIsbn13(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getThumbnail(),
                reviewCount
        );
    }
}
