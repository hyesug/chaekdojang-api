package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.admin.dto.BookReviewStatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    Page<Review> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Review> findForUpdateByIdAndDeletedAtIsNull(@Param("id") Long id);

    Optional<Review> findByIdAndDeletedAtIsNullAndHiddenFalse(Long id);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long authorId);

    long countByAuthorIdAndDeletedAtIsNullAndHiddenFalse(Long authorId);

    long countByBookIdAndDeletedAtIsNullAndHiddenFalse(Long bookId);

    long countByCreatedAtBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT r.author.id) FROM Review r WHERE r.book.id = :bookId AND r.deletedAt IS NULL AND r.hidden = false")
    long countReadersByBookId(@Param("bookId") Long bookId);

    @Query("""
            SELECT r.author.id, COUNT(r), MAX(r.createdAt)
            FROM Review r
            WHERE r.deletedAt IS NULL
              AND r.hidden = false
              AND r.author.deletedAt IS NULL
              AND r.author.id NOT IN :excludeIds
            GROUP BY r.author.id
            ORDER BY COUNT(r) DESC, MAX(r.createdAt) DESC
            """)
    List<Object[]> findTopAuthorStatsByReviewCount(@Param("excludeIds") List<Long> excludeIds);

    List<Review> findAllByAuthorIdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<Long> authorIds);

    List<Review> findAllByBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long bookId);

    Page<Review> findAllByDeletedAtIsNullAndHiddenFalse(Pageable pageable);

    List<Review> findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(List<Long> authorIds);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long authorId);

    List<Review> findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long bookId);

    List<Review> findAllByAuthorIdAndBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long authorId, Long bookId);

    List<Review> findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long authorId, Long bookId);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long authorId);

    List<Review> findAllByAuthorIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long authorId, LocalDateTime start, LocalDateTime end);

    List<Review> findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> authorIds, LocalDateTime start, LocalDateTime end);

    List<Review> findAllBySourceReview_Author_IdAndDeletedAtIsNullAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long authorId, LocalDateTime start, LocalDateTime end);

    List<Review> findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long authorId, Long bookId);

    boolean existsByPreviousReviewIdAndDeletedAtIsNull(Long previousReviewId);

    List<Review> findAllBySourceReviewIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long sourceReviewId);

    boolean existsBySourceReviewIdAndDeletedAtIsNull(Long sourceReviewId);

    List<Review> findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByRatingDescCreatedAtDesc(Long bookId);

    List<Review> findTop5ByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(Long bookId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.book.id = :bookId
              AND r.deletedAt IS NULL
              AND r.hidden = false
            ORDER BY (
                r.viewCount * 1
                + (SELECT COUNT(c) FROM Comment c WHERE c.review = r AND c.deletedAt IS NULL) * 5
                + (SELECT COUNT(rl) FROM ReviewLike rl WHERE rl.review = r) * 3
                + CASE
                    WHEN r.createdAt >= :weekAgo THEN 10
                    WHEN r.createdAt >= :monthAgo THEN 5
                    ELSE 0
                  END
            ) DESC, r.createdAt DESC
            """)
    List<Review> findAllByBookIdOrderByPopularity(
            @Param("bookId") Long bookId,
            @Param("weekAgo") LocalDateTime weekAgo,
            @Param("monthAgo") LocalDateTime monthAgo);

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
              AND r1.hidden = false AND r2.hidden = false
            GROUP BY r2.author.id
            """)
    List<Object[]> findRatingSimilarity(@Param("myId") Long myId, @Param("excludeIds") List<Long> excludeIds);

    @Query(value = """
            SELECT r2.book_id, COUNT(DISTINCT r2.author_id) AS shared_readers
            FROM reviews r1
            JOIN reviews r2 ON r2.author_id = r1.author_id
            WHERE r1.book_id = :bookId
              AND r2.book_id IS NOT NULL
              AND r2.book_id <> :bookId
              AND r1.deleted_at IS NULL AND r2.deleted_at IS NULL
              AND r1.hidden = FALSE AND r2.hidden = FALSE
            GROUP BY r2.book_id
            HAVING COUNT(DISTINCT r2.author_id) >= 2
            ORDER BY shared_readers DESC, r2.book_id ASC
            LIMIT 6
            """, nativeQuery = true)
    List<Object[]> findConnectedBookStats(@Param("bookId") Long bookId);

    // 내 독후감 페이징 + 키워드 검색 (내용 또는 책 제목)
    // CAST(:q AS string) — null 시 PostgreSQL이 bytea로 추론하는 문제 방지
    @Query("SELECT r FROM Review r LEFT JOIN r.book b " +
           "WHERE r.author.id = :userId AND r.deletedAt IS NULL " +
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

    @Query(value = """
            SELECT r FROM Review r
            WHERE r.deletedAt IS NULL AND r.hidden = false
            ORDER BY (
                r.viewCount * 1
                + (SELECT COUNT(c) FROM Comment c WHERE c.review = r AND c.deletedAt IS NULL) * 5
                + (SELECT COUNT(rl) FROM ReviewLike rl WHERE rl.review = r) * 3
                + CASE
                    WHEN r.createdAt >= :weekAgo THEN 10
                    WHEN r.createdAt >= :monthAgo THEN 5
                    ELSE 0
                  END
            ) DESC, r.createdAt DESC
            """,
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.deletedAt IS NULL AND r.hidden = false")
    Page<Review> findAllByPopularity(
            @Param("weekAgo") LocalDateTime weekAgo,
            @Param("monthAgo") LocalDateTime monthAgo,
            Pageable pageable);

    @Query("SELECT new com.chaekdojang.api.domain.admin.dto.BookReviewStatResponse(" +
           "b.id, b.title, b.author, COUNT(r)) " +
           "FROM Review r JOIN r.book b " +
           "WHERE r.deletedAt IS NULL AND r.hidden = false " +
           "GROUP BY b.id, b.title, b.author " +
           "ORDER BY COUNT(r) DESC")
    List<BookReviewStatResponse> findBookReviewStats();

    /** 예약 시각이 지난 독후감 — 스케줄러가 하나씩 공개한다 */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id FROM reviews
            WHERE scheduled_at IS NOT NULL AND scheduled_at <= :now AND deleted_at IS NULL
            ORDER BY scheduled_at LIMIT 200""", nativeQuery = true)
    List<Long> findDueScheduledIds(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    /**
     * 예약 독후감을 공개한다. created_at 은 엔티티에서 바꿀 수 없게 막혀 있어(updatable=false) 여기서 옮긴다.
     * 아직 예약 상태인 행만 바꾸므로, 그사이 취소·비공개 전환된 글은 0을 돌려준다.
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE reviews SET hidden = false, created_at = scheduled_at, updated_at = scheduled_at, scheduled_at = NULL
            WHERE id = :id AND scheduled_at IS NOT NULL AND deleted_at IS NULL""", nativeQuery = true)
    int publishScheduled(@org.springframework.data.repository.query.Param("id") Long id);
}
