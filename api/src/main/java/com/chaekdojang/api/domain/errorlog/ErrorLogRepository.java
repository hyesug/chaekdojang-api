package com.chaekdojang.api.domain.errorlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    @Query("""
            SELECT e FROM ErrorLog e
            WHERE (:q = ''
                   OR LOWER(e.uri) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.exceptionType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.message) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR e.ip LIKE CONCAT('%', :q, '%'))
              AND e.uri NOT LIKE '/api/admin%'
              AND (:level = '' OR e.level = :level)
              AND (:statusMin < 0 OR e.status >= :statusMin)
              AND (:statusMax < 0 OR e.status < :statusMax)
            """)
    Page<ErrorLog> search(
            @Param("q") String q,
            @Param("level") String level,
            @Param("statusMin") Integer statusMin,
            @Param("statusMax") Integer statusMax,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM ErrorLog e
            WHERE e.createdAt >= :since
              AND e.uri NOT LIKE '/api/admin%'
              AND (e.userId IS NULL OR e.userId NOT IN :adminIds)
            """)
    List<ErrorLog> findVisibleSince(
            @Param("since") LocalDateTime since,
            @Param("adminIds") List<Long> adminIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ErrorLog e WHERE e.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
