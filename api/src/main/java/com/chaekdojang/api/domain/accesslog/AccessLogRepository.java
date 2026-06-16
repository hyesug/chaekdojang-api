package com.chaekdojang.api.domain.accesslog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    Page<AccessLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT a FROM AccessLog a
            WHERE (:q IS NULL OR LOWER(a.uri) LIKE LOWER(CONCAT('%', :q, '%')) OR a.ip LIKE CONCAT('%', :q, '%'))
              AND (:method IS NULL OR a.method = :method)
              AND (:statusMin IS NULL OR a.status >= :statusMin)
              AND (:statusMax IS NULL OR a.status < :statusMax)
            """)
    Page<AccessLog> search(
            @Param("q") String q,
            @Param("method") String method,
            @Param("statusMin") Integer statusMin,
            @Param("statusMax") Integer statusMax,
            Pageable pageable
    );
}
