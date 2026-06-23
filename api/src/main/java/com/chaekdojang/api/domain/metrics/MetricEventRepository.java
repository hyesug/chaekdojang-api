package com.chaekdojang.api.domain.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricEventRepository extends JpaRepository<MetricEvent, Long> {
    List<MetricEvent> findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc();

    @Query("""
            SELECT m FROM MetricEvent m
            LEFT JOIN m.user u
            WHERE (:q = ''
                   OR LOWER(m.path) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.sessionId) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.referrer, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.ip, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
              AND (:eventType = '' OR m.eventType = :eventType)
              AND (:userType = ''
                   OR (:userType = 'member' AND u IS NOT NULL)
                   OR (:userType = 'guest' AND u IS NULL))
            """)
    Page<MetricEvent> search(
            @Param("q") String q,
            @Param("eventType") String eventType,
            @Param("userType") String userType,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MetricEvent m SET m.user = null WHERE m.user.id = :userId")
    void anonymizeUser(@Param("userId") Long userId);
}
