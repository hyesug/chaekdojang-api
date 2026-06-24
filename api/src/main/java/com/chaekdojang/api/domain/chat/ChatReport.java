package com.chaekdojang.api.domain.chat;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_reports_message_reporter", columnNames = {"message_id", "reporter_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ChatReport of(ChatMessage message, User reporter, String reason) {
        ChatReport report = new ChatReport();
        report.message = message;
        report.reporter = reporter;
        report.reason = reason;
        report.createdAt = LocalDateTime.now();
        return report;
    }
}
