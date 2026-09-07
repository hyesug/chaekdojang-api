package com.chaekdojang.api.domain.dojangdan;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 선정자 1명의 전자책 접근 권한.
 * 열람 로그는 유출이 발생했을 때 추적할 유일한 근거다.
 */
@Entity
@Table(name = "ebook_access_grants")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class EbookAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private ReviewCampaignApplication application;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime firstOpenedAt;

    @Column
    private LocalDateTime lastOpenedAt;

    @Column(nullable = false)
    private int openCount;

    @Column(length = 64)
    private String lastOpenIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WatermarkStatus watermarkStatus = WatermarkStatus.PENDING;

    @Column(length = 500)
    private String watermarkedStorageKey;

    @Column
    private LocalDateTime revokedAt;

    @Builder
    private EbookAccessGrant(ReviewCampaignApplication application, LocalDateTime expiresAt) {
        this.application = application;
        this.expiresAt = expiresAt;
        this.grantedAt = LocalDateTime.now();
        this.watermarkStatus = WatermarkStatus.PENDING;
    }

    public void markWatermarkReady(String storageKey) {
        this.watermarkedStorageKey = storageKey;
        this.watermarkStatus = WatermarkStatus.READY;
    }

    public void markWatermarkFailed() {
        this.watermarkStatus = WatermarkStatus.FAILED;
    }

    public void recordOpen(String ip) {
        LocalDateTime now = LocalDateTime.now();
        if (this.firstOpenedAt == null) {
            this.firstOpenedAt = now;
        }
        this.lastOpenedAt = now;
        this.lastOpenIp = ip;
        this.openCount++;
    }

    /** 서평단 취소·중도 포기 시 즉시 차단 */
    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
        }
    }

    public void extendUntil(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isReadable(LocalDateTime now) {
        return !isRevoked() && !isExpired(now);
    }
}
