package com.chaekdojang.api.domain.readinggroup.dto;

import com.chaekdojang.api.domain.readinggroup.ReadingGroupQuestionResponse;

import java.time.LocalDateTime;

public record ReadingGroupQuestionAnswerResponse(
        Long id,
        Long userId,
        String nickname,
        String profileImage,
        String content,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReadingGroupQuestionAnswerResponse from(
            ReadingGroupQuestionResponse value, Long currentUserId) {
        return new ReadingGroupQuestionAnswerResponse(
                value.getId(),
                value.getUser().getId(),
                value.getUser().getDeletedAt() == null ? value.getUser().getNickname() : "탈퇴한 사용자",
                value.getUser().getDeletedAt() == null ? value.getUser().getProfileImage() : null,
                value.getContent(),
                currentUserId != null && currentUserId.equals(value.getUser().getId()),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }
}
