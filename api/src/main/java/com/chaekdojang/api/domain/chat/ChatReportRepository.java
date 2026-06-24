package com.chaekdojang.api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {
    boolean existsByMessageIdAndReporterId(Long messageId, Long reporterId);
}
