package com.chaekdojang.api.domain.book.dto;

public record BookConnectionResponse(
        Long bookId,
        String title,
        String author,
        String thumbnail,
        long sharedReaderCount,
        String reason
) {
}
