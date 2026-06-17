package com.chaekdojang.api.domain.accesslog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    @Query("""
            SELECT a FROM AccessLog a
            WHERE (:q = '' OR LOWER(a.uri) LIKE LOWER(CONCAT('%', :q, '%')) OR a.ip LIKE CONCAT('%', :q, '%'))
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
}
