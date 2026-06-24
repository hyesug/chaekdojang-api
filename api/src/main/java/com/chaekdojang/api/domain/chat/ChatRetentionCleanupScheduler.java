package com.chaekdojang.api.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRetentionCleanupScheduler {

    private final ChatService chatService;

    @Value("${app.chat.retention-days:180}")
    private int retentionDays;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldMessages() {
        if (retentionDays <= 0) return;
        int deleted = chatService.deleteMessagesOlderThanDays(retentionDays);
        if (deleted > 0) {
            log.info("Deleted {} old chat messages older than {} days.", deleted, retentionDays);
        }
    }
}
