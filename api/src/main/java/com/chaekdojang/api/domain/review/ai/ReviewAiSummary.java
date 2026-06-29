package com.chaekdojang.api.domain.review.ai;

import com.chaekdojang.api.domain.review.Review;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "review_ai_summaries")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewAiSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(length = 60)
    private String oneLineReview;

    @ElementCollection
    @CollectionTable(name = "review_ai_summary_emotion_keywords", joinColumns = @JoinColumn(name = "summary_id"))
    @Column(name = "keyword", nullable = false)
    private List<String> emotionKeywords = new ArrayList<>();

    @Column(length = 120)
    private String recommendedFor;

    @Column(length = 100)
    private String impressivePoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewAiSummaryStatus status = ReviewAiSummaryStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewAiSummarySource summarySource = ReviewAiSummarySource.AI;

    @Column(nullable = false)
    private boolean userEdited = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    @Builder
    private ReviewAiSummary(Review review) {
        this.review = review;
    }

    public void markPending() {
        this.status = ReviewAiSummaryStatus.PENDING;
        this.errorMessage = null;
    }

    public void markProcessing() {
        this.status = ReviewAiSummaryStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void complete(AiSummaryResult result, int retryCount) {
        this.oneLineReview = result.oneLineReview();
        this.emotionKeywords = new ArrayList<>(result.emotionKeywords());
        this.recommendedFor = result.recommendedFor();
        this.impressivePoint = result.impressivePoint();
        this.status = ReviewAiSummaryStatus.COMPLETED;
        this.summarySource = ReviewAiSummarySource.AI;
        this.userEdited = false;
        this.retryCount = retryCount;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void edit(AiSummaryResult result) {
        this.oneLineReview = result.oneLineReview();
        this.emotionKeywords = new ArrayList<>(result.emotionKeywords());
        this.recommendedFor = result.recommendedFor();
        this.impressivePoint = result.impressivePoint();
        this.status = ReviewAiSummaryStatus.EDITED;
        this.summarySource = ReviewAiSummarySource.USER_EDITED;
        this.userEdited = true;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String message, int retryCount) {
        this.status = ReviewAiSummaryStatus.FAILED;
        this.errorMessage = truncate(message, 1000);
        this.retryCount = retryCount;
    }

    public void updateRetryCount(int retryCount, String message) {
        this.retryCount = retryCount;
        this.errorMessage = truncate(message, 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
