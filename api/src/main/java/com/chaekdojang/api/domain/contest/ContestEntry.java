package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.book.Book;
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
 * 공모전 응모작 1건. 한 사람은 공모전마다 한 편만 낸다.
 * 독후감 연결형이면 review에, 전용 글 작성형이면 title·content에 값이 들어간다.
 */
@Entity
@Table(name = "contest_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contest_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ContestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 독후감 연결형 응모작 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /** 어느 지정 도서로 응모했는지. 자유주제면 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestEntryStatus status = ContestEntryStatus.SUBMITTED;

    /** 1이 최상위 등수 */
    @Column
    private Integer awardRank;

    @Column(length = 50)
    private String awardName;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime withdrawnAt;

    @Column
    private LocalDateTime judgedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ContestEntry(Contest contest, User user, Review review, Book book,
                         String title, String content) {
        this.contest = contest;
        this.user = user;
        this.review = review;
        this.book = book;
        this.title = title;
        this.content = content;
        this.status = ContestEntryStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /** 취소했던 응모를 같은 행에 다시 채워 넣는다. 사람당 한 행만 두기 때문이다. */
    public void resubmit(Review review, Book book, String title, String content) {
        this.review = review;
        this.book = book;
        this.title = title;
        this.content = content;
        this.status = ContestEntryStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.withdrawnAt = null;
    }

    public void withdraw() {
        this.status = ContestEntryStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }

    public void award(int awardRank, String awardName) {
        this.status = ContestEntryStatus.AWARDED;
        this.awardRank = awardRank;
        this.awardName = awardName;
        this.judgedAt = LocalDateTime.now();
    }

    /** 수상 지정을 되돌린다. 발표 전 재심사에서 쓴다. */
    public void clearAward() {
        this.status = ContestEntryStatus.SUBMITTED;
        this.awardRank = null;
        this.awardName = null;
        this.judgedAt = null;
    }

    public void markNotAwarded() {
        this.status = ContestEntryStatus.NOT_AWARDED;
        this.judgedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
