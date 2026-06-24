package com.chaekdojang.api.domain.chat;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.chat.dto.ChatMessageRequest;
import com.chaekdojang.api.domain.chat.dto.ChatMessageResponse;
import com.chaekdojang.api.domain.chat.dto.ChatReportRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatBlockRepository chatBlockRepository;
    private final ChatReportRepository chatReportRepository;

    public ChatRoom getOrCreateRoom(Long bookId) {
        return chatRoomRepository.findByBookId(bookId).orElseGet(() -> {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
            return chatRoomRepository.save(ChatRoom.create(book));
        });
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long bookId, int page) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatRoom room = chatRoomRepository.findByBookId(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdDesc(
                room.getId(), PageRequest.of(page, 50));
        return messages.reversed().stream()
                .filter(message -> !isBlockedBetween(userId, message.getSender().getId()))
                .map(ChatMessageResponse::from)
                .toList();
    }

    public ChatMessageResponse sendMessage(Long bookId, Long userId, ChatMessageRequest req) {
        String content = normalizeContent(req.content());
        ChatRoom room = getOrCreateRoom(bookId);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (sender.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.builder()
                        .chatRoom(room)
                        .sender(sender)
                        .content(content)
                        .build()
        );

        ChatMessageResponse response = ChatMessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/chat/" + bookId, response);
        return response;
    }

    @Transactional
    public void reportMessage(Long messageId, ChatReportRequest request) {
        Long reporterId = SecurityUtils.getCurrentUserId();
        if (chatReportRepository.existsByMessageIdAndReporterId(messageId, reporterId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        chatReportRepository.save(ChatReport.of(message, reporter, normalizeReason(request.reason())));
    }

    @Transactional
    public void blockUser(Long blockedUserId) {
        Long blockerId = SecurityUtils.getCurrentUserId();
        if (blockerId.equals(blockedUserId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (chatBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) return;
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        chatBlockRepository.save(ChatBlock.of(blocker, blocked));
    }

    @Transactional
    public void unblockUser(Long blockedUserId) {
        Long blockerId = SecurityUtils.getCurrentUserId();
        chatBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedUserId);
    }

    @Transactional
    public int deleteMessagesOlderThanDays(int retentionDays) {
        return chatMessageRepository.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
    }

    private String normalizeContent(String value) {
        String content = value == null ? "" : value.trim();
        if (content.isBlank() || content.length() > 1000) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return content;
    }

    private String normalizeReason(String value) {
        String reason = value == null ? "" : value.trim();
        if (reason.length() < 5 || reason.length() > 500) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return reason;
    }

    private boolean isBlockedBetween(Long userId, Long otherUserId) {
        return chatBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                userId, otherUserId, otherUserId, userId);
    }
}
