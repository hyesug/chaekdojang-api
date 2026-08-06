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
@Table(name = "review_follow_up_questions")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReviewFollowUpQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private Review review;

    @Column(nullable = false, length = 600)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewAiSummarySource source;

    @Column(nullable = false, length = 64)
    private String sourceHash;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 30)
    private String promptVersion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ReviewFollowUpQuestion create(
            Review review, String question, String sourceHash, String model, String promptVersion) {
        ReviewFollowUpQuestion value = new ReviewFollowUpQuestion();
        value.review = review;
        value.applyAi(question, sourceHash, model, promptVersion);
        return value;
    }

    public void applyAi(String question, String sourceHash, String model, String promptVersion) {
        this.question = question;
        this.source = ReviewAiSummarySource.AI;
        this.sourceHash = sourceHash;
        this.model = model;
        this.promptVersion = promptVersion;
    }

    public void edit(String question) {
        this.question = question;
        this.source = ReviewAiSummarySource.USER_EDITED;
    }

    public boolean isUserEdited() {
        return source == ReviewAiSummarySource.USER_EDITED;
    }
}
