package com.chaekdojang.api.domain.accesslog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    @Query("""
            SELECT a FROM AccessLog a
            LEFT JOIN a.user u
            WHERE (:q = '' OR LOWER(a.uri) LIKE LOWER(CONCAT('%', :q, '%')) OR a.ip LIKE CONCAT('%', :q, '%'))
              AND a.uri NOT LIKE '/api/admin%'
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
              AND (:method = '' OR a.method = :method)
              AND (:statusMin < 0 OR a.status >= :statusMin)
              AND (:statusMax < 0 OR a.status < :statusMax)
            """)
    Page<AccessLog> search(
            @Param("q") String q,
            @Param("method") String method,
            @Param("statusMin") Integer statusMin,
            @Param("statusMax") Integer statusMax,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.user u
            WHERE a.createdAt >= :since
              AND a.uri NOT LIKE '/api/admin%'
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
            """)
    List<AccessLog> findVisibleSince(@Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccessLog a WHERE a.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.user u
            WHERE a.createdAt >= :since
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
              AND a.method = 'GET'
              AND a.status >= 200 AND a.status < 400
              AND a.uri NOT LIKE '/api/users/me%'
              AND (a.uri LIKE '/api/books/%'
                   OR a.uri LIKE '/api/reviews/%'
                   OR a.uri LIKE '/api/profiles/%'
                   OR a.uri LIKE '/api/users/%')
            """)
    List<AccessLog> findRecentPublicReads(@Param("since") LocalDateTime since);
}
