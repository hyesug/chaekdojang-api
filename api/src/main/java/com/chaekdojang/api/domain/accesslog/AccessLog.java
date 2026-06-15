package com.chaekdojang.api.domain.accesslog;

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

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private long elapsedMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AccessLog(String ip, String method, String uri, int status, long elapsedMs) {
        this.ip = ip;
        this.method = method;
        this.uri = uri;
        this.status = status;
        this.elapsedMs = elapsedMs;
        this.createdAt = LocalDateTime.now(KST);
    }
}
