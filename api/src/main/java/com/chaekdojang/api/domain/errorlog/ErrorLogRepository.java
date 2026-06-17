package com.chaekdojang.api.domain.errorlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    @Query("""
            SELECT e FROM ErrorLog e
            WHERE (CAST(:q AS String) IS NULL
                   OR LOWER(e.uri) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.exceptionType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.message) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR e.ip LIKE CONCAT('%', :q, '%'))
              AND (CAST(:level AS String) IS NULL OR e.level = :level)
              AND (:statusMin IS NULL OR e.status >= :statusMin)
              AND (:statusMax IS NULL OR e.status < :statusMax)
            """)
    Page<ErrorLog> search(
            @Param("q") String q,
            @Param("level") String level,
            @Param("statusMin") Integer statusMin,
            @Param("statusMax") Integer statusMax,
            Pageable pageable
    );
}
