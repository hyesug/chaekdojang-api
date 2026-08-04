package com.chaekdojang.api.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);

    boolean existsByNickname(String nickname);

    List<User> findByNicknameContainingIgnoreCaseAndDeletedAtIsNull(String nickname);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    List<User> findAllByLifeBook_IdAndDeletedAtIsNull(Long lifeBookId);

    long countByCreatedAtBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end);

    List<User> findAllByRoleInAndDeletedAtIsNull(List<UserRole> roles);

    List<User> findTop20ByDeletedAtIsNullOrderByCreatedAtDesc();

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:q = '' OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR (:qUserId IS NOT NULL AND u.id = :qUserId))
              AND (:ip = '' OR EXISTS (
                    SELECT m.id FROM MetricEvent m
                    WHERE m.user = u AND m.ip LIKE CONCAT('%', :ip, '%')))
              AND (:deviceId = '' OR EXISTS (
                    SELECT m.id FROM MetricEvent m
                    WHERE m.user = u AND m.deviceId LIKE CONCAT('%', :deviceId, '%')))
              AND u.createdAt >= :joinedFrom
              AND u.createdAt < :joinedTo
              AND (:activeFilter = false OR EXISTS (
                    SELECT m.id FROM MetricEvent m
                    WHERE m.user = u
                      AND m.createdAt >= :activeFrom
                      AND m.createdAt < :activeTo))
              AND (:createdGroup IS NULL
                   OR (:createdGroup = true AND EXISTS (SELECT g.id FROM ReadingGroup g WHERE g.owner = u))
                   OR (:createdGroup = false AND NOT EXISTS (SELECT g.id FROM ReadingGroup g WHERE g.owner = u)))
              AND (:hasRelated IS NULL
                   OR (:hasRelated = true AND EXISTS (
                        SELECT m1.id FROM MetricEvent m1
                        WHERE m1.user = u
                          AND m1.createdAt >= :relatedSince
                          AND EXISTS (
                            SELECT m2.id FROM MetricEvent m2
                            WHERE m2.user.id <> u.id
                              AND m2.user.deletedAt IS NULL
                              AND m2.createdAt >= :relatedSince
                              AND ((m1.deviceId IS NOT NULL AND m1.deviceId <> '' AND m2.deviceId = m1.deviceId)
                                   OR (m1.ip IS NOT NULL AND m1.ip <> '' AND m2.ip = m1.ip)))))
                   OR (:hasRelated = false AND NOT EXISTS (
                        SELECT m1.id FROM MetricEvent m1
                        WHERE m1.user = u
                          AND m1.createdAt >= :relatedSince
                          AND EXISTS (
                            SELECT m2.id FROM MetricEvent m2
                            WHERE m2.user.id <> u.id
                              AND m2.user.deletedAt IS NULL
                              AND m2.createdAt >= :relatedSince
                              AND ((m1.deviceId IS NOT NULL AND m1.deviceId <> '' AND m2.deviceId = m1.deviceId)
                                   OR (m1.ip IS NOT NULL AND m1.ip <> '' AND m2.ip = m1.ip))))))
            """)
    Page<User> searchForAdmin(
            @Param("q") String q,
            @Param("qUserId") Long qUserId,
            @Param("ip") String ip,
            @Param("deviceId") String deviceId,
            @Param("joinedFrom") LocalDateTime joinedFrom,
            @Param("joinedTo") LocalDateTime joinedTo,
            @Param("activeFrom") LocalDateTime activeFrom,
            @Param("activeTo") LocalDateTime activeTo,
            @Param("activeFilter") boolean activeFilter,
            @Param("relatedSince") LocalDateTime relatedSince,
            @Param("hasRelated") Boolean hasRelated,
            @Param("createdGroup") Boolean createdGroup,
            Pageable pageable
    );
}
