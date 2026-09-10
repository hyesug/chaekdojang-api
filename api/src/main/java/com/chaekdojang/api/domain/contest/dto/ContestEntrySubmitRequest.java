package com.chaekdojang.api.domain.contest.dto;

import jakarta.validation.constraints.Size;

/**
 * 응모 요청.
 * 독후감 연결형은 reviewId만, 전용 글 작성형은 title·content를 채워 보낸다.
 */
public record ContestEntrySubmitRequest(
        Long reviewId,
        Long bookId,
        @Size(max = 200) String title,
        @Size(max = 30000) String content,
        boolean agreeTerms
) {
}
