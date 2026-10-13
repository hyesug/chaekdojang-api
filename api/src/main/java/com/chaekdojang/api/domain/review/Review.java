package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_review_id")
    private Review previousReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_review_id")
    private Review sourceReview;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column
    private long viewCount = 0;

    @Column(length = 500)
    private String keywords;

    @Column(nullable = false)
    private boolean spoiler = false;

    /** 예약 발송 시각(KST). 이 시각 전에는 비공개로 두고 스케줄러가 공개한다. null 이면 예약 없음 */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Builder
    private Review(Book book, User author, Review previousReview, Review sourceReview,
                   String content, int rating, String keywords, boolean spoiler) {
        this.book = book;
        this.author = author;
        this.previousReview = previousReview;
        this.sourceReview = sourceReview;
        this.content = content;
        this.rating = rating;
        this.keywords = keywords;
        this.spoiler = spoiler;
    }

    public void update(String content, int rating, Book book) {
        this.content = content;
        this.rating = rating;
        this.book = book;
    }

    public void updateDiscoveryMetadata(String keywords, boolean spoiler) {
        this.keywords = keywords;
        this.spoiler = spoiler;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }

    /** 비공개로 돌리면 예약도 함께 거둔다 — 나중에 저절로 공개되지 않게 */
    public void hide() { this.hidden = true; this.scheduledAt = null; }
    /** 지금 공개하면 예약은 의미가 없어진다 */
    public void unhide() { this.hidden = false; this.scheduledAt = null; }
    /** 예약: 그 시각까지 비공개로 둔다 */
    public void schedule(LocalDateTime at) { this.hidden = true; this.scheduledAt = at; }
    public boolean isScheduled() { return scheduledAt != null; }
    public void increaseViewCount() { this.viewCount++; }
}
