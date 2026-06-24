package com.chaekdojang.api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatBlockRepository extends JpaRepository<ChatBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(Long blockerId, Long blockedId, Long reverseBlockerId, Long reverseBlockedId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
