package com.chaekdojang.api.domain.readinggroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingGroupMemberRepository extends JpaRepository<ReadingGroupMember, Long> {
    Optional<ReadingGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, ReadingGroupMemberStatus status);
    List<ReadingGroupMember> findAllByGroupIdOrderByCreatedAtAsc(Long groupId);
    List<ReadingGroupMember> findAllByGroupIdAndStatusOrderByCreatedAtAsc(Long groupId, ReadingGroupMemberStatus status);
}
