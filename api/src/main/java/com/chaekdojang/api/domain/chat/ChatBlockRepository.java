package com.chaekdojang.api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatBlockRepository extends JpaRepository<ChatBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(Long blockerId, Long blockedId, Long reverseBlockerId, Long reverseBlockedId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
            FROM ChatBlock b
            WHERE b.blocker.id = :userId OR b.blocked.id = :userId
            """)
    List<Long> findBlockedUserIds(@Param("userId") Long userId);
}
