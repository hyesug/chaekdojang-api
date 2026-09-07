package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.review.Review;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 독후감 활용 동의 1건.
 * 동의 내용(항목·표기 방식·약관 버전)은 만든 뒤 바꾸지 않는다.
 * 동의를 바꾸려면 이 행을 revoke하고 새 행을 만든다.
 */
@Entity
@Table(name = "review_usage_consents")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewUsageConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private ReviewCampaignApplication application;

    // 동의는 독후감 작성 전에 받으므로 처음에는 비어 있다. 제출 시점에 연결한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(nullable = false)
    private boolean consentPromotional;

    @Column(nullable = false)
    private boolean consentExcerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentDisplayNameType displayNameType = ConsentDisplayNameType.REAL_NICKNAME;

    @Column(nullable = false, length = 40)
    private String termsVersion;

    @Column(nullable = false)
    private LocalDateTime consentedAt;

    @Column(length = 64)
    private String consentIp;

    @Column
    private LocalDateTime revokedAt;

    @Builder
    private ReviewUsageConsent(ReviewCampaignApplication application, boolean consentPromotional,
                               boolean consentExcerpt, ConsentDisplayNameType displayNameType,
                               String termsVersion, String consentIp) {
        this.application = application;
        this.consentPromotional = consentPromotional;
        this.consentExcerpt = consentExcerpt;
        this.displayNameType = displayNameType == null
                ? ConsentDisplayNameType.REAL_NICKNAME
                : displayNameType;
        this.termsVersion = termsVersion;
        this.consentIp = consentIp;
        this.consentedAt = LocalDateTime.now();
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
        }
    }

    /** 제출된 독후감을 연결한다. 동의 내용 자체는 바뀌지 않는다. */
    public void linkReview(Review review) {
        this.review = review;
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    /** 지금 이 독후감을 홍보에 쓸 수 있는지 */
    public boolean allowsPromotionalUse() {
        return isActive() && consentPromotional;
    }
}
