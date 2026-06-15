package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "metric_events", indexes = {
        @Index(name = "idx_metric_events_created_at", columnList = "created_at"),
        @Index(name = "idx_metric_events_session_id", columnList = "session_id"),
        @Index(name = "idx_metric_events_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MetricEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String sessionId;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 500)
    private String referrer;

    @Column(nullable = false)
    private long durationMs;

    @Column(length = 80)
    private String device;

    @Column(length = 50)
    private String ip;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MetricEvent(User user, String eventType, String sessionId, String path,
                        String referrer, long durationMs, String device, String ip) {
        this.user = user;
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.path = path;
        this.referrer = referrer;
        this.durationMs = durationMs;
        this.device = device;
        this.ip = ip;
    }
}
