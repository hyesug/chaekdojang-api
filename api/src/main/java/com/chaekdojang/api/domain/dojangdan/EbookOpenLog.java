package com.chaekdojang.api.domain.dojangdan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "ebook_open_logs")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class EbookOpenLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grant_id", nullable = false)
    private EbookAccessGrant grant;

    @Column(nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(length = 64, updatable = false)
    private String ip;

    @Column(nullable = false, length = 500, updatable = false)
    private String sourceStorageKey;

    public EbookOpenLog(EbookAccessGrant grant, String ip, String sourceStorageKey) {
        this.grant = grant;
        this.openedAt = grant.getLastOpenedAt();
        this.ip = ip;
        this.sourceStorageKey = sourceStorageKey;
    }
}
