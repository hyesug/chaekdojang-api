package com.chaekdojang.api.domain.metrics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface MetricEventRepository extends JpaRepository<MetricEvent, Long> {
    List<MetricEvent> findTop1000ByUserIsNotNullAndIpIsNotNullOrderByCreatedAtDesc();

    @Query("""
            SELECT m.user.id, MAX(m.createdAt)
            FROM MetricEvent m
            WHERE m.user.id IN :userIds
            GROUP BY m.user.id
            """)
    List<Object[]> findLastActivityByUserIds(@Param("userIds") List<Long> userIds);

    @Query("""
            SELECT DISTINCT m.ip FROM MetricEvent m
            JOIN m.user u
            WHERE m.ip IS NOT NULL
              AND u.role <> com.chaekdojang.api.domain.user.UserRole.USER
            """)
    List<String> findAdminIps();

    @Query("""
            SELECT m FROM MetricEvent m
            LEFT JOIN m.user u
            WHERE (:q = ''
                   OR LOWER(m.path) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.sessionId) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.referrer, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.ip, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.eventType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.deviceId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.device, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.browser, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(m.operatingSystem, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(u.nickname, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
              AND m.ip NOT IN :excludedIps
              AND (:excludedIpPrefix = '' OR m.ip IS NULL OR m.ip NOT LIKE CONCAT(:excludedIpPrefix, '%'))
              AND (:eventType = '' OR m.eventType = :eventType)
              AND (:excludeBackground = false OR m.eventType NOT IN ('heartbeat', 'session_end'))
              AND (:userType = ''
                   OR (:userType = 'member' AND u IS NOT NULL)
                   OR (:userType = 'guest' AND u IS NULL))
            """)
    Page<MetricEvent> search(
            @Param("q") String q,
            @Param("eventType") String eventType,
            @Param("userType") String userType,
            @Param("excludeBackground") boolean excludeBackground,
            @Param("excludedIps") List<String> excludedIps,
            @Param("excludedIpPrefix") String excludedIpPrefix,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM MetricEvent m
            LEFT JOIN FETCH m.user u
            WHERE m.createdAt >= :since
              AND (u IS NULL OR u.role = com.chaekdojang.api.domain.user.UserRole.USER)
              AND m.ip NOT IN :excludedIps
              AND (:excludedIpPrefix = '' OR m.ip IS NULL OR m.ip NOT LIKE CONCAT(:excludedIpPrefix, '%'))
              AND m.path NOT LIKE '/admin%'
              AND m.path NOT LIKE '/api/admin%'
            """)
    List<MetricEvent> findVisibleSince(
            @Param("since") LocalDateTime since,
            @Param("excludedIps") List<String> excludedIps,
            @Param("excludedIpPrefix") String excludedIpPrefix
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MetricEvent m SET m.user = null WHERE m.user.id = :userId")
    void anonymizeUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MetricEvent m WHERE m.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    long countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(Long userId, String eventType, LocalDateTime createdAt);

    @Query("""
            SELECT m FROM MetricEvent m
            WHERE m.user.id = :userId
              AND (:eventType = '' OR m.eventType = :eventType)
              AND (:from IS NULL OR m.createdAt >= :from)
              AND (:to IS NULL OR m.createdAt < :to)
            """)
    Page<MetricEvent> findUserTimeline(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    List<MetricEvent> findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId, LocalDateTime createdAt);

    @Query("""
            SELECT m FROM MetricEvent m JOIN FETCH m.user u
            WHERE u.id <> :userId
              AND u.deletedAt IS NULL
              AND u.role = com.chaekdojang.api.domain.user.UserRole.USER
              AND m.deviceId IN :deviceIds
              AND m.createdAt >= :since
            """)
    List<MetricEvent> findRelatedByDeviceIds(
            @Param("userId") Long userId,
            @Param("deviceIds") List<String> deviceIds,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT m FROM MetricEvent m JOIN FETCH m.user u
            WHERE u.id <> :userId
              AND u.deletedAt IS NULL
              AND u.role = com.chaekdojang.api.domain.user.UserRole.USER
              AND m.ip IN :ips
              AND m.createdAt >= :since
            """)
    List<MetricEvent> findRelatedByIps(
            @Param("userId") Long userId,
            @Param("ips") List<String> ips,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT m FROM MetricEvent m JOIN FETCH m.user u
            WHERE u.id <> :userId
              AND u.deletedAt IS NULL
              AND u.role = com.chaekdojang.api.domain.user.UserRole.USER
              AND m.eventType IN ('reading_group_created', 'reading_group_joined', 'reading_group_join_requested')
              AND m.createdAt >= :since
            """)
    List<MetricEvent> findRelatedGroupActivity(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    Optional<MetricEvent> findFirstByUserIdAndEventTypeOrderByCreatedAtAsc(Long userId, String eventType);
    Optional<MetricEvent> findFirstByUserIdAndEventTypeOrderByCreatedAtDesc(Long userId, String eventType);
    Optional<MetricEvent> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(m1) > 0 THEN true ELSE false END
            FROM MetricEvent m1
            WHERE m1.user.id = :userId
              AND m1.createdAt >= :since
              AND EXISTS (
                    SELECT m2.id FROM MetricEvent m2
                    WHERE m2.user.id <> :userId
                      AND m2.user.deletedAt IS NULL
                      AND m2.createdAt >= :since
                      AND ((m1.deviceId IS NOT NULL AND m1.deviceId <> '' AND m2.deviceId = m1.deviceId)
                           OR (m1.ip IS NOT NULL AND m1.ip <> '' AND m2.ip = m1.ip)))
            """)
    boolean existsRelatedSignal(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
