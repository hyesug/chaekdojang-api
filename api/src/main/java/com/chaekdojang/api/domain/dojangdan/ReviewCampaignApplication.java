package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 서평단 신청 1건.
 * 단계별 시각(applied/selected/rejected/submitted/dropped)을 남겨 완주율 지표의 원천으로 쓴다.
 * 나중에 소급 집계가 불가능하므로 상태 전이마다 반드시 시각을 기록한다.
 */
@Entity
@Table(name = "review_campaign_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewCampaignApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private ReviewCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignApplicationStatus status = CampaignApplicationStatus.APPLIED;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    @Column
    private LocalDateTime selectedAt;

    @Column
    private LocalDateTime rejectedAt;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime droppedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ReviewCampaignApplication(ReviewCampaign campaign, User user, String message) {
        this.campaign = campaign;
        this.user = user;
        this.message = message;
        this.status = CampaignApplicationStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
    }

    public void select() {
        this.status = CampaignApplicationStatus.SELECTED;
        this.selectedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = CampaignApplicationStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void submit(Review review) {
        this.review = review;
        this.status = CampaignApplicationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void drop() {
        this.status = CampaignApplicationStatus.DROPPED;
        this.droppedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
