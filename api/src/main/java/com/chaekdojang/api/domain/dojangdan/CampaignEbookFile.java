package com.chaekdojang.api.domain.dojangdan;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "campaign_ebook_files")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CampaignEbookFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private ReviewCampaign campaign;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long byteSize;

    @Column
    private Integer pageCount;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Builder
    private CampaignEbookFile(ReviewCampaign campaign, String storageKey, String originalFilename,
                              String mimeType, long byteSize, Integer pageCount) {
        this.campaign = campaign;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.byteSize = byteSize;
        this.pageCount = pageCount;
    }

    /** 파일을 새로 올리면 기존 행을 교체한다. 캠페인당 파일은 하나만 둔다. */
    public void replace(String storageKey, String originalFilename, String mimeType,
                        long byteSize, Integer pageCount) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.byteSize = byteSize;
        this.pageCount = pageCount;
        this.uploadedAt = LocalDateTime.now();
    }
}
