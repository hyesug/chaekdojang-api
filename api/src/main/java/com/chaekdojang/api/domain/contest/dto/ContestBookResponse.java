package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.book.Book;

/** 공모전 지정 도서 */
public record ContestBookResponse(
        Long bookId,
        String title,
        String author,
        String thumbnail
) {
    public static ContestBookResponse from(Book book) {
        return new ContestBookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getThumbnail());
    }
}
