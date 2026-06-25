package com.chaekdojang.api.domain.notification;

import com.chaekdojang.api.domain.notification.dto.NotificationResponse;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 알림 생성 (내부 호출용)
    @Transactional
    public void send(User receiver, User sender, NotificationType type, Long targetId) {
        send(receiver, sender, type, targetId, null);
    }

    @Transactional
    public void send(User receiver, User sender, NotificationType type, Long targetId, String targetSlug) {
        // 자기 자신에게는 알림 보내지 않음
        if (receiver.getId().equals(sender.getId())) return;
        notificationRepository.save(
                Notification.builder()
                        .receiver(receiver)
                        .sender(sender)
                        .type(type)
                        .targetId(targetId)
                        .targetSlug(targetSlug)
                        .build()
        );
    }

    // 내 알림 목록 조회
    public List<NotificationResponse> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(100)
                .map(NotificationResponse::from)
                .toList();
    }

    // 읽지 않은 알림 수
    public long getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    // 전체 읽음 처리
    @Transactional
    public void markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.findAllByReceiverIdOrderByCreatedAtDesc(userId)
                .forEach(Notification::markAsRead);
    }

    // 전체 삭제
    @Transactional
    public void deleteAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.deleteAllByReceiverId(userId);
    }

    // 알림 삭제
    @Transactional
    public void delete(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        notificationRepository.delete(notification);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
