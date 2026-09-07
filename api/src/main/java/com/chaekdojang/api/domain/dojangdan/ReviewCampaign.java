package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "review_campaigns")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private OfficialProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int recruitCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime recruitStartAt;

    @Column(nullable = false)
    private LocalDateTime recruitEndAt;

    @Column(nullable = false)
    private LocalDateTime reviewDueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignDeliveryType deliveryType = CampaignDeliveryType.PHYSICAL;

    /** 전자책 열람 만료 = 독후감 마감일 + 이 일수 */
    @Column(nullable = false)
    private int ebookAccessExtraDays = 7;

    /** 공개 모집 전 관심 독자에게만 열어두는 시간. 0이면 우선 초대 없이 바로 공개한다. */
    @Column(nullable = false)
    private int priorityInviteHours = 24;

    @Column
    private LocalDateTime priorityInviteUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ReviewCampaign(OfficialProfile profile, Book book, String title, String description,
                           int recruitCount, LocalDateTime recruitStartAt, LocalDateTime recruitEndAt,
                           LocalDateTime reviewDueAt, Integer priorityInviteHours,
                           CampaignDeliveryType deliveryType, Integer ebookAccessExtraDays) {
        this.profile = profile;
        this.book = book;
        this.title = title;
        this.description = description;
        this.recruitCount = recruitCount;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.reviewDueAt = reviewDueAt;
        this.priorityInviteHours = priorityInviteHours == null ? 24 : priorityInviteHours;
        this.deliveryType = deliveryType == null ? CampaignDeliveryType.PHYSICAL : deliveryType;
        this.ebookAccessExtraDays = ebookAccessExtraDays == null ? 7 : ebookAccessExtraDays;
        this.status = CampaignStatus.DRAFT;
    }

    /** 선정자에게 열어줄 전자책 열람 만료 시각 */
    public LocalDateTime ebookExpiresAt() {
        return reviewDueAt.plusDays(ebookAccessExtraDays);
    }

    public void update(String title, String description, int recruitCount,
                       LocalDateTime recruitStartAt, LocalDateTime recruitEndAt,
                       LocalDateTime reviewDueAt, Integer priorityInviteHours,
                       CampaignDeliveryType deliveryType, Integer ebookAccessExtraDays) {
        this.title = title;
        this.description = description;
        this.recruitCount = recruitCount;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.reviewDueAt = reviewDueAt;
        if (priorityInviteHours != null) {
            this.priorityInviteHours = priorityInviteHours;
        }
        if (deliveryType != null) {
            this.deliveryType = deliveryType;
        }
        if (ebookAccessExtraDays != null) {
            this.ebookAccessExtraDays = ebookAccessExtraDays;
        }
    }

    public void changeStatus(CampaignStatus status) {
        this.status = status;
    }

    /** 모집을 시작하면서 우선 초대 기간을 연다. 이미 열린 적 있으면 다시 열지 않는다. */
    public void startRecruiting() {
        this.status = CampaignStatus.RECRUITING;
        if (priorityInviteHours > 0 && priorityInviteUntil == null) {
            this.priorityInviteUntil = LocalDateTime.now().plusHours(priorityInviteHours);
        }
    }

    /** 지금이 관심 독자 우선 신청 기간인지 */
    public boolean isInPriorityWindow(LocalDateTime now) {
        return priorityInviteUntil != null && now.isBefore(priorityInviteUntil);
    }

    /** 독자에게 목록·상세를 공개할 수 있는 상태인지 */
    public boolean isPublic() {
        return status != CampaignStatus.DRAFT;
    }

    /** 지금 신청을 받을 수 있는지 */
    public boolean isAcceptingApplications(LocalDateTime now) {
        return status == CampaignStatus.RECRUITING
                && !now.isBefore(recruitStartAt)
                && !now.isAfter(recruitEndAt);
    }
}
