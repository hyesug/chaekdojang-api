package com.chaekdojang.api.domain.admin.audit;

import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class AdminAuditLog {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 50)
    private String targetType;

    @Column
    private Long targetId;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static AdminAuditLog create(User actor, String action, String targetType, Long targetId, String summary) {
        AdminAuditLog log = new AdminAuditLog();
        log.actor = actor;
        log.action = action;
        log.targetType = targetType;
        log.targetId = targetId;
        log.summary = summary != null && summary.length() > 1000 ? summary.substring(0, 1000) : summary;
        log.createdAt = LocalDateTime.now(KST);
        return log;
    }
}
