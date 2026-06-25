package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.review.Review;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_group_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_book_id", "review_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_book_id", nullable = false)
    private ReadingGroupBook groupBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReadingGroupReview of(ReadingGroup group, ReadingGroupBook groupBook, Review review) {
        ReadingGroupReview item = new ReadingGroupReview();
        item.group = group;
        item.groupBook = groupBook;
        item.review = review;
        return item;
    }
}
