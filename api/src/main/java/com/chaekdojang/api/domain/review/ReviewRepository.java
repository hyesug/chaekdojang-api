package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.admin.dto.BookReviewStatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Page<Review> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long authorId);

    long countByAuthorIdAndDeletedAtIsNull(Long authorId);

    long countByBookIdAndDeletedAtIsNullAndHiddenFalse(Long bookId);

    List<Review> findAllByAuthorIdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<Long> authorIds);

    List<Review> findAllByBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long bookId);

    Page<Review> findAllByDeletedAtIsNullAndHiddenFalse(Pageable pageable);

    List<Review> findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(List<Long> authorIds);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long authorId);

    List<Review> findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long bookId);

    @Query("SELECT r FROM Review r JOIN r.book b " +
           "WHERE r.deletedAt IS NULL AND r.hidden = false " +
           "AND LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
           "AND REPLACE(LOWER(b.author), ' ', '') LIKE LOWER(CONCAT('%', :author, '%')) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findAllByBookTitleAndAuthorLike(
            @Param("title") String title,
            @Param("author") String author);

    // 나와 같은 책을 읽고 별점을 남긴 유저별 유사도 점수 반환
    // 별점 동일: 2점, 차이 1: 1점, 차이 2 이상: 0점
    @Query("""
            SELECT r2.author.id,
                   SUM(CASE WHEN ABS(r1.rating - r2.rating) = 0 THEN 2
                            WHEN ABS(r1.rating - r2.rating) = 1 THEN 1
                            ELSE 0 END)
            FROM Review r1
            JOIN Review r2 ON r1.book = r2.book
            WHERE r1.author.id = :myId
              AND r2.author.id NOT IN :excludeIds
              AND r1.deletedAt IS NULL AND r2.deletedAt IS NULL
            GROUP BY r2.author.id
            """)
    List<Object[]> findRatingSimilarity(@Param("myId") Long myId, @Param("excludeIds") List<Long> excludeIds);

    // 내 독후감 페이징 + 키워드 검색 (내용 또는 책 제목)
    // CAST(:q AS string) — null 시 PostgreSQL이 bytea로 추론하는 문제 방지
    @Query("SELECT r FROM Review r LEFT JOIN r.book b " +
           "WHERE r.author.id = :userId AND r.deletedAt IS NULL AND r.hidden = false " +
           "AND (CAST(:q AS string) IS NULL " +
           "     OR LOWER(r.content) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) " +
           "     OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findByAuthorWithSearch(
            @Param("userId") Long userId,
            @Param("q") String q,
            Pageable pageable);

    // 관리자용: 작성자 닉네임·책 제목 검색 + 페이징 (삭제되지 않은 전체)
    @Query("SELECT r FROM Review r LEFT JOIN r.book b LEFT JOIN r.author u " +
           "WHERE r.deletedAt IS NULL " +
           "AND (CAST(:author AS string) IS NULL OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', CAST(:author AS string), '%'))) " +
           "AND (CAST(:title AS string) IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findAllByDeletedAtIsNullWithSearch(
            @Param("author") String author,
            @Param("title") String title,
            Pageable pageable);

    @Query(value = "SELECT r FROM Review r WHERE r.deletedAt IS NULL AND r.hidden = false " +
                   "ORDER BY (SELECT COUNT(rl) FROM ReviewLike rl WHERE rl.review = r) DESC, r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.deletedAt IS NULL AND r.hidden = false")
    Page<Review> findAllByPopularity(Pageable pageable);

    @Query("SELECT new com.chaekdojang.api.domain.admin.dto.BookReviewStatResponse(" +
           "b.id, b.title, b.author, COUNT(r)) " +
           "FROM Review r JOIN r.book b " +
           "WHERE r.deletedAt IS NULL " +
           "GROUP BY b.id, b.title, b.author " +
           "ORDER BY COUNT(r) DESC")
    List<BookReviewStatResponse> findBookReviewStats();
}
