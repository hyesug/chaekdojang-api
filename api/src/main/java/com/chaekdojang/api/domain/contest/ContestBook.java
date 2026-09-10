package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.book.Book;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 공모전 지정 도서.
 * 한 건도 없으면 자유주제 공모전이다.
 */
@Entity
@Table(name = "contest_books",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contest_id", "book_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ContestBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ContestBook of(Contest contest, Book book) {
        ContestBook contestBook = new ContestBook();
        contestBook.contest = contest;
        contestBook.book = book;
        return contestBook;
    }
}
