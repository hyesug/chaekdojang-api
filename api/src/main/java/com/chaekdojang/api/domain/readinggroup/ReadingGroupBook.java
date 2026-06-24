package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "reading_group_books",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "book_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReadingGroupBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(length = 200)
    private String note;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReadingGroupBook of(ReadingGroup group, Book book, String note) {
        ReadingGroupBook item = new ReadingGroupBook();
        item.group = group;
        item.book = book;
        item.note = note;
        return item;
    }
}
