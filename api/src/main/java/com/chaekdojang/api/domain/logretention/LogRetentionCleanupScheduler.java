package com.chaekdojang.api.domain.logretention;

import com.chaekdojang.api.domain.accesslog.AccessLogRepository;
import com.chaekdojang.api.domain.errorlog.ErrorLogRepository;
import com.chaekdojang.api.domain.fortune.feedback.FortuneFeedbackRepository;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final FortuneFeedbackRepository fortuneFeedbackRepository;

    @Value("${app.user-activity.retention-days:90}")
    private int userActivityRetentionDays;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldLogs() {
        try {
            LocalDateTime now = LocalDateTime.now(KST);
            int retentionDays = Math.max(userActivityRetentionDays, 1);
            int accessDeleted = accessLogRepository.deleteCreatedBefore(now.minusDays(retentionDays));
            int metricDeleted = metricEventRepository.deleteCreatedBefore(now.minusDays(retentionDays));
            int errorDeleted = errorLogRepository.deleteCreatedBefore(now.minusDays(180));
            // 운세·AI 답변 피드백은 수집일로부터 1년 보관(개인정보처리방침)
            int feedbackDeleted = fortuneFeedbackRepository.deleteCreatedBefore(now.minusYears(1));
            log.info("Log retention cleanup completed: accessLogs={}, metricEvents={}, errorLogs={}, fortuneFeedback={}",
                    accessDeleted, metricDeleted, errorDeleted, feedbackDeleted);
        } catch (Exception e) {
            log.warn("Log retention cleanup failed: {}", e.getMessage(), e);
        }
    }
}
