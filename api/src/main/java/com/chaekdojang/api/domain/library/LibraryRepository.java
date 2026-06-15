package com.chaekdojang.api.domain.library;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibraryRepository extends JpaRepository<Library, Long> {

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    List<Library> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Library> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, LibraryStatus status);

    Optional<Library> findByIdAndUserId(Long id, Long userId);

    Optional<Library> findByUserIdAndBookId(Long userId, Long bookId);

    long countByUserIdAndStatus(Long userId, LibraryStatus status);

    @Query("SELECT l.book.id FROM Library l WHERE l.user.id = :userId")
    List<Long> findBookIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT l.user.id, COUNT(l) as overlapCount
            FROM Library l
            WHERE l.book.id IN :bookIds
            AND l.user.id NOT IN :excludeUserIds
            GROUP BY l.user.id
            ORDER BY COUNT(l) DESC
            """)
    List<Object[]> findUsersWithMostBookOverlap(
            @Param("bookIds") List<Long> bookIds,
            @Param("excludeUserIds") List<Long> excludeUserIds
    );

    @Query(value = """
            SELECT EXTRACT(YEAR FROM updated_at) as year,
                   EXTRACT(MONTH FROM updated_at) as month,
                   COUNT(*) as count
            FROM libraries
            WHERE user_id = :userId AND status = 'FINISHED'
            GROUP BY EXTRACT(YEAR FROM updated_at), EXTRACT(MONTH FROM updated_at)
            ORDER BY year DESC, month DESC
            LIMIT 12
            """, nativeQuery = true)
    List<Object[]> findMonthlyReadingStats(@Param("userId") Long userId);

    @Query(value = """
            SELECT b.category, COUNT(*) as count
            FROM libraries l
            JOIN books b ON l.book_id = b.id
            WHERE l.user_id = :userId AND l.status = 'FINISHED' AND b.category IS NOT NULL
            GROUP BY b.category
            ORDER BY count DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findGenreStats(@Param("userId") Long userId);
}
