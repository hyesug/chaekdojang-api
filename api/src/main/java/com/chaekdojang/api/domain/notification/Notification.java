package com.chaekdojang.api.domain.notification;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column
    private Long targetId; // 좋아요·댓글의 경우 reviewId

    @Column(length = 120)
    private String targetSlug;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(User receiver, User sender, NotificationType type, Long targetId, String targetSlug) {
        this.receiver = receiver;
        this.sender = sender;
        this.type = type;
        this.targetId = targetId;
        this.targetSlug = targetSlug;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
