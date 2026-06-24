package com.chaekdojang.api.domain.logretention;

import com.chaekdojang.api.domain.accesslog.AccessLogRepository;
import com.chaekdojang.api.domain.errorlog.ErrorLogRepository;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class LogRetentionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionCleanupScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AccessLogRepository accessLogRepository;
    private final MetricEventRepository metricEventRepository;
    private final ErrorLogRepository errorLogRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldLogs() {
        try {
            LocalDateTime now = LocalDateTime.now(KST);
            int accessDeleted = accessLogRepository.deleteCreatedBefore(now.minusDays(30));
            int metricDeleted = metricEventRepository.deleteCreatedBefore(now.minusDays(90));
            int errorDeleted = errorLogRepository.deleteCreatedBefore(now.minusDays(180));
            log.info("Log retention cleanup completed: accessLogs={}, metricEvents={}, errorLogs={}",
                    accessDeleted, metricDeleted, errorDeleted);
        } catch (Exception e) {
            log.warn("Log retention cleanup failed: {}", e.getMessage(), e);
        }
    }
}
