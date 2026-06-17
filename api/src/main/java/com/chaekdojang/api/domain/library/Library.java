package com.chaekdojang.api.domain.library;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "libraries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LibraryStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDate completedAt;

    @Builder
    private Library(User user, Book book, LibraryStatus status, LocalDate completedAt) {
        this.user = user;
        this.book = book;
        this.status = status;
        if (status == LibraryStatus.FINISHED) {
            this.completedAt = completedAt != null ? completedAt : LocalDate.now();
        }
    }

    public void updateStatus(LibraryStatus status, LocalDate completedAt) {
        this.status = status;
        if (status == LibraryStatus.FINISHED) {
            this.completedAt = completedAt != null
                    ? completedAt
                    : (this.completedAt != null ? this.completedAt : LocalDate.now());
        } else {
            this.completedAt = null;
        }
    }
}
