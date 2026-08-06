package com.chaekdojang.api.domain.review.reflection;

import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummarySource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "review_change_comparisons")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewChangeComparison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_review_id", nullable = false)
    private Review previousReview;

    @Column(nullable = false, length = 800) private String previousFocus;
    @Column(nullable = false, length = 800) private String currentFocus;
    @Column(nullable = false, length = 800) private String sharedThought;
    @Column(nullable = false, length = 800) private String changedPerspective;
    @Column(nullable = false, length = 800) private String newElement;
    @Column(nullable = false, length = 600) private String reflectionQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewAiSummarySource source;

    @Column(nullable = false, length = 64) private String sourceHash;
    @Column(nullable = false, length = 100) private String model;
    @Column(nullable = false, length = 30) private String promptVersion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReviewChangeComparison create(
            Review review, Review previousReview, ChangeComparisonResult result,
            String sourceHash, String model, String promptVersion) {
        ReviewChangeComparison value = new ReviewChangeComparison();
        value.review = review;
        value.previousReview = previousReview;
        value.applyAi(result, sourceHash, model, promptVersion);
        return value;
    }

    public void applyAi(ChangeComparisonResult result, String sourceHash, String model, String promptVersion) {
        apply(result);
        this.source = ReviewAiSummarySource.AI;
        this.sourceHash = sourceHash;
        this.model = model;
        this.promptVersion = promptVersion;
    }

    public void edit(ChangeComparisonResult result) {
        apply(result);
        this.source = ReviewAiSummarySource.USER_EDITED;
    }

    private void apply(ChangeComparisonResult result) {
        this.previousFocus = result.previousFocus();
        this.currentFocus = result.currentFocus();
        this.sharedThought = result.sharedThought();
        this.changedPerspective = result.changedPerspective();
        this.newElement = result.newElement();
        this.reflectionQuestion = result.reflectionQuestion();
    }

    public boolean isUserEdited() {
        return source == ReviewAiSummarySource.USER_EDITED;
    }
}
