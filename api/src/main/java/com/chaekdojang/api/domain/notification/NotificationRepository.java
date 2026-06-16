package com.chaekdojang.api.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    void deleteAllByReceiverId(Long receiverId);

    void deleteAllByReceiverIdOrSenderId(Long receiverId, Long senderId);
}
