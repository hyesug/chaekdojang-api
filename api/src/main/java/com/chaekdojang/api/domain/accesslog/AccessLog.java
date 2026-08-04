package com.chaekdojang.api.domain.accesslog;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "access_logs")
@Getter
@NoArgsConstructor
public class AccessLog {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private long elapsedMs;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "device_id", length = 80)
    private String deviceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AccessLog(String ip, User user, String method, String uri, int status, long elapsedMs,
                     String userAgent, String deviceId) {
        this.ip = ip;
        this.user = user;
        this.method = method;
        this.uri = uri;
        this.status = status;
        this.elapsedMs = elapsedMs;
        this.userAgent = userAgent;
        this.deviceId = deviceId;
        this.createdAt = LocalDateTime.now(KST);
    }
}
