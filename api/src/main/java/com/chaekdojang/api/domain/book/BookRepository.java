package com.chaekdojang.api.domain.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn13(String isbn13);

    Optional<Book> findFirstBySlugAndDeletedAtIsNullAndIsPublicTrueOrderByIdAsc(String slug);

    Optional<Book> findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc(String title);

    List<Book> findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc();

    List<Book> findAllByCategoryContainingIgnoreCase(String category);

    @Query("""
            SELECT b FROM Book b
            WHERE (:title = '' OR REPLACE(LOWER(COALESCE(b.title, '')), ' ', '') LIKE CONCAT('%', :title, '%')
                   OR LOWER(COALESCE(b.isbn13, '')) LIKE CONCAT('%', :title, '%'))
              AND (:author = '' OR REPLACE(LOWER(COALESCE(b.author, '')), ' ', '') LIKE CONCAT('%', :author, '%'))
              AND (:publisher = '' OR REPLACE(LOWER(COALESCE(b.publisher, '')), ' ', '') LIKE CONCAT('%', :publisher, '%'))
            """)
    List<Book> searchByFilters(
            @Param("title") String title,
            @Param("author") String author,
            @Param("publisher") String publisher);
}
