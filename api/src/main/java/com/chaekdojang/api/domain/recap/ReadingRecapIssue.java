package com.chaekdojang.api.domain.recap;

import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_recap_issues", uniqueConstraints =
        @UniqueConstraint(name = "uk_reading_recap_issue_period", columnNames = {"user_id", "period_key"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingRecapIssue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 40) private String periodKey;
    @Column(nullable = false) private LocalDate periodStart;
    @Column(nullable = false) private LocalDate periodEnd;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 1000) private String summary;
    @Column(nullable = false) private int ownReviewCount;
    @Column(nullable = false) private int followingReviewCount;
    @Column(nullable = false) private int savedReviewCount;
    @Column(nullable = false) private int continuedReviewCount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "featured_review_id") private Review featuredReview;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "memory_review_id") private Review memoryReview;
    @Column(length = 300) private String memoryBookTitle;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column private LocalDateTime readAt;

    public static ReadingRecapIssue create(
            User user, String periodKey, LocalDate periodStart, LocalDate periodEnd,
            String title, String summary, int ownReviewCount, int followingReviewCount,
            int savedReviewCount, int continuedReviewCount, Review featuredReview,
            Review memoryReview, String memoryBookTitle) {
        ReadingRecapIssue value = new ReadingRecapIssue();
        value.user = user;
        value.periodKey = periodKey;
        value.periodStart = periodStart;
        value.periodEnd = periodEnd;
        value.title = title;
        value.summary = summary;
        value.ownReviewCount = ownReviewCount;
        value.followingReviewCount = followingReviewCount;
        value.savedReviewCount = savedReviewCount;
        value.continuedReviewCount = continuedReviewCount;
        value.featuredReview = featuredReview;
        value.memoryReview = memoryReview;
        value.memoryBookTitle = memoryBookTitle;
        return value;
    }

    public void markRead() { if (readAt == null) readAt = LocalDateTime.now(); }
}
