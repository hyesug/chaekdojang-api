package com.chaekdojang.api.domain.notification.dto;

import com.chaekdojang.api.domain.notification.Notification;
import com.chaekdojang.api.domain.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String senderNickname,
        String senderProfileImage,
        Long targetId,
        String targetSlug,
        String message,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        String senderNickname = n.getSender().getDeletedAt() != null ? "탈퇴한 사용자" : n.getSender().getNickname();
        String senderProfileImage = n.getSender().getDeletedAt() != null ? null : n.getSender().getProfileImage();
        String message = switch (n.getType()) {
            case LIKE    -> senderNickname + "님이 독후감에 좋아요를 눌렀어요";
            case COMMENT -> senderNickname + "님이 댓글을 달았어요";
            case FOLLOW  -> senderNickname + "님이 팔로우하기 시작했어요";
            case SAME_BOOK_REVIEW -> senderNickname + "님이 내가 읽은 책에 새 독후감을 남겼어요";
            case GROUP_JOIN_REQUEST -> senderNickname + "님이 독서모임 가입을 신청했어요";
            case GROUP_JOINED -> senderNickname + "님이 독서모임에 가입했어요";
        };
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                senderNickname,
                senderProfileImage,
                n.getTargetId(),
                n.getTargetSlug(),
                message,
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
