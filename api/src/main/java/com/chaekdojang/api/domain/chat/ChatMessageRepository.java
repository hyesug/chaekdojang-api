package com.chaekdojang.api.domain.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender WHERE m.chatRoom.id = :roomId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByRoomIdDesc(@Param("roomId") Long roomId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
