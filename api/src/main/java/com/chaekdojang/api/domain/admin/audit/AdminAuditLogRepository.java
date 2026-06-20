package com.chaekdojang.api.domain.admin.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("""
            SELECT l FROM AdminAuditLog l
            LEFT JOIN l.actor actor
            WHERE (:q = ''
                   OR LOWER(l.action) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(l.targetType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(l.summary) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(actor.nickname) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:action = '' OR l.action = :action)
              AND (:targetType = '' OR l.targetType = :targetType)
            """)
    Page<AdminAuditLog> search(
            @Param("q") String q,
            @Param("action") String action,
            @Param("targetType") String targetType,
            Pageable pageable
    );
}
