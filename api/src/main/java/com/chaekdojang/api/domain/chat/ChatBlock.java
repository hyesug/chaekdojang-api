package com.chaekdojang.api.domain.chat;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_blocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_blocks_pair", columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ChatBlock of(User blocker, User blocked) {
        ChatBlock block = new ChatBlock();
        block.blocker = blocker;
        block.blocked = blocked;
        block.createdAt = LocalDateTime.now();
        return block;
    }
}
