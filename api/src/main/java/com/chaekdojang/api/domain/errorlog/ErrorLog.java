package com.chaekdojang.api.domain.errorlog;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "error_logs")
@Getter
@NoArgsConstructor
public class ErrorLog {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false, length = 200)
    private String exceptionType;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 50)
    private String ip;

    @Column
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ErrorLog(String level, String method, String uri, int status,
                     String exceptionType, String message, String ip, Long userId) {
        this.level = level;
        this.method = method;
        this.uri = uri;
        this.status = status;
        this.exceptionType = exceptionType;
        this.message = message;
        this.ip = ip;
        this.userId = userId;
        this.createdAt = LocalDateTime.now(KST);
    }
}
