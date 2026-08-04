package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReadingGroupMemberRepository extends JpaRepository<ReadingGroupMember, Long> {
    Optional<ReadingGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, ReadingGroupMemberStatus status);
    List<ReadingGroupMember> findAllByGroupIdOrderByCreatedAtAsc(Long groupId);
    List<ReadingGroupMember> findAllByGroupIdAndStatusOrderByCreatedAtAsc(Long groupId, ReadingGroupMemberStatus status);
    List<ReadingGroupMember> findAllByUserIdAndStatusInOrderByUpdatedAtDesc(Long userId, Collection<ReadingGroupMemberStatus> statuses);
    long countByGroupId(Long groupId);
    long countByGroupIdAndStatus(Long groupId, ReadingGroupMemberStatus status);
    void deleteAllByGroupId(Long groupId);

    @Query("""
            SELECT COUNT(m) FROM ReadingGroupMember m
            WHERE m.user.id = :userId
              AND m.status = com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberStatus.APPROVED
              AND m.role <> com.chaekdojang.api.domain.readinggroup.ReadingGroupMemberRole.OWNER
            """)
    long countApprovedMembershipsByUserId(@Param("userId") Long userId);
}
