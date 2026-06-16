package com.chaekdojang.api.domain.chat.dto;

import com.chaekdojang.api.domain.chat.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long senderId,
        String senderNickname,
        String senderProfileImage,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        boolean deletedSender = m.getSender().getDeletedAt() != null;
        return new ChatMessageResponse(
                m.getId(),
                deletedSender ? null : m.getSender().getId(),
                deletedSender ? "탈퇴한 사용자" : m.getSender().getNickname(),
                deletedSender ? null : m.getSender().getProfileImage(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
