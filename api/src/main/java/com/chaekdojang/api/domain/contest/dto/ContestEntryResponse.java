package com.chaekdojang.api.domain.contest.dto;

import com.chaekdojang.api.domain.contest.ContestEntry;
import com.chaekdojang.api.domain.contest.ContestEntryStatus;
import com.chaekdojang.api.domain.review.Review;

import java.time.LocalDateTime;

/** 주최자가 심사할 때 보는 응모작 1건 */
public record ContestEntryResponse(
        Long entryId,
        Long userId,
        String nickname,
        String profileImage,
        ContestEntryStatus status,
        LocalDateTime submittedAt,
        Long bookId,
        String bookTitle,
        String entryTitle,
        String content,
        int contentLength,
        Long reviewId,
        boolean reviewDeleted,
        Integer awardRank,
        String awardName
) {
    public static ContestEntryResponse from(ContestEntry entry) {
        Review review = entry.getReview();
        boolean reviewDeleted = review != null && review.getDeletedAt() != null;
        // 독후감 연결형이면 독후감 본문을, 전용 글 작성형이면 응모 본문을 그대로 심사 화면에 내려준다.
        String content = review == null ? entry.getContent() : (reviewDeleted ? null : review.getContent());
        return new ContestEntryResponse(
                entry.getId(),
                entry.getUser().getId(),
                entry.getUser().getNickname(),
                entry.getUser().getProfileImage(),
                entry.getStatus(),
                entry.getSubmittedAt(),
                entry.getBook() == null ? null : entry.getBook().getId(),
                entry.getBook() == null ? null : entry.getBook().getTitle(),
                entry.getTitle(),
                content,
                content == null ? 0 : content.length(),
                review == null ? null : review.getId(),
                reviewDeleted,
                entry.getAwardRank(),
                entry.getAwardName()
        );
    }
}
