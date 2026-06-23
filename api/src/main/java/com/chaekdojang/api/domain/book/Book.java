package com.chaekdojang.api.domain.book;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 13)
    private String isbn13;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column
    private String publisher;

    @Column
    private String thumbnail;

    @Column(length = 160)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Integer publishedYear;

    @Column(length = 200)
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookSource source;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    private Book(String isbn13, String title, String author, String publisher,
                 String thumbnail, String slug, String description, Integer publishedYear,
                 String seoTitle, String seoDescription, BookSource source, String category) {
        this.isbn13 = isbn13;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.thumbnail = thumbnail;
        this.slug = slug;
        this.description = description;
        this.publishedYear = publishedYear;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.source = source;
        this.category = category;
    }

    public void updateSeoFields(String slug, String description, String seoTitle, String seoDescription) {
        if (slug != null && !slug.isBlank()) this.slug = slug;
        if (description != null && !description.isBlank()) this.description = description;
        if (seoTitle != null && !seoTitle.isBlank()) this.seoTitle = seoTitle;
        if (seoDescription != null && !seoDescription.isBlank()) this.seoDescription = seoDescription;
    }
}
