package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.book.Book;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "official_profile_books",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "book_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OfficialProfileBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private OfficialProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static OfficialProfileBook of(OfficialProfile profile, Book book) {
        OfficialProfileBook item = new OfficialProfileBook();
        item.profile = profile;
        item.book = book;
        return item;
    }
}
