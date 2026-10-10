package com.chaekdojang.api.domain.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn13(String isbn13);

    Optional<Book> findBySourceAndExternalId(BookSource source, String externalId);

    Optional<Book> findFirstBySlugAndDeletedAtIsNullAndIsPublicTrueOrderByIdAsc(String slug);

    Optional<Book> findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc(String title);

    List<Book> findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc();

    List<Book> findAllByCategoryContainingIgnoreCase(String category);

    /** 아직 주제 태그를 붙여 보지 않은, 소개글이 있는 책 — 추천 대상이 아닌 종류는 뺀다 */
    @Query("""
            SELECT b FROM Book b
            WHERE b.themesTaggedAt IS NULL AND b.deletedAt IS NULL AND b.isPublic = true
              AND b.description IS NOT NULL AND LENGTH(b.description) >= 40
              AND (b.category IS NULL OR b.category NOT IN :excluded)
            ORDER BY b.id DESC
            """)
    List<Book> findUntaggedForThemes(@Param("excluded") List<String> excluded, org.springframework.data.domain.Pageable page);

    @Query("SELECT COUNT(b) FROM Book b JOIN b.themes t WHERE t = :theme AND b.deletedAt IS NULL AND b.isPublic = true")
    long countByTheme(@Param("theme") String theme);

    /** 그 주제가 붙은 책 — 책도장 독후감이 많은 순 */
    @Query("""
            SELECT b, COUNT(r.id) FROM Book b JOIN b.themes t
            LEFT JOIN com.chaekdojang.api.domain.review.Review r
                   ON r.book = b AND r.deletedAt IS NULL AND r.hidden = false
            WHERE t = :theme AND b.deletedAt IS NULL AND b.isPublic = true
              AND (b.category IS NULL OR b.category NOT IN :excluded)
            GROUP BY b
            ORDER BY COUNT(r.id) DESC, b.id DESC
            """)
    List<Object[]> findByThemeRanked(@Param("theme") String theme, @Param("excluded") List<String> excluded,
                                     org.springframework.data.domain.Pageable page);

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
