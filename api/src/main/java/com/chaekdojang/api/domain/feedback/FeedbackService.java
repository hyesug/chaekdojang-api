package com.chaekdojang.api.domain.feedback;

import com.chaekdojang.api.domain.feedback.dto.FeedbackCommentRequest;
import com.chaekdojang.api.domain.feedback.dto.FeedbackCommentResponse;
import com.chaekdojang.api.domain.feedback.dto.FeedbackConfigResponse;
import com.chaekdojang.api.domain.feedback.dto.FeedbackResult;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String PATH = "/write";

    private final FeedbackProperties properties;
    private final OpenAiFeedbackClient openAiFeedbackClient;
    private final MetricEventRepository metricEventRepository;
    private final MetricEventService metricEventService;

    public FeedbackConfigResponse getConfig() {
        return new FeedbackConfigResponse(
                properties.getMinChars(),
                properties.getMaxChars(),
                properties.getBoundaryMessage(),
                properties.getBetaApplyUrl() == null || properties.getBetaApplyUrl().isBlank()
                        ? null
                        : properties.getBetaApplyUrl(),
                properties.getMemberDailyLimit() > 0
        );
    }

    @Transactional
    public FeedbackCommentResponse createReviewComment(FeedbackCommentRequest request, String ip) {
        Long userId = SecurityUtils.getCurrentUserId();
        String content = request.content().trim();
        validateEnabled();
        validateContentLength(content);
        assertDailyLimit(userId);

        record("feedback_requested", request.sessionId(), ip, userId, Map.of("contentLength", content.length()));

        try {
            FeedbackResult feedback = openAiFeedbackClient.createFeedback(content);
            record("feedback_succeeded", request.sessionId(), ip, userId, Map.of(
                    "contentLength", content.length(),
                    "notReview", feedback.notReview()
            ));
            return FeedbackCommentResponse.of(
                    feedback,
                    properties.getMinChars(),
                    properties.getMaxChars(),
                    properties.getBoundaryMessage(),
                    properties.getBetaApplyUrl()
            );
        } catch (Exception e) {
            log.warn("feedback failed", e);
            record("feedback_failed", request.sessionId(), ip, userId, Map.of(
                    "contentLength", content.length(),
                    "reason", e.getClass().getSimpleName()
            ));
            throw new CustomException(ErrorCode.FEEDBACK_FAILED);
        }
    }

    private void validateEnabled() {
        if (!properties.isEnabled()) {
            throw new CustomException(ErrorCode.FEEDBACK_DISABLED);
        }
    }

    private void validateContentLength(String content) {
        int length = content.length();
        if (length < properties.getMinChars() || length > properties.getMaxChars()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void assertDailyLimit(Long userId) {
        int limit = properties.getMemberDailyLimit();
        if (limit <= 0) {
            return;
        }
        LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay();
        long used = List.of("feedback_succeeded", "reflection_ai_succeeded",
                        "reading_group_ai_question_succeeded", "reading_group_ai_analysis_succeeded")
                .stream()
                .mapToLong(eventType -> metricEventRepository
                        .countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(
                                userId, eventType, todayStart))
                .sum();
        if (used >= limit) {
            throw new CustomException(ErrorCode.FEEDBACK_LIMIT_EXCEEDED);
        }
    }

    private void record(String eventType, String sessionId, String ip, Long userId, Map<String, Object> meta) {
        metricEventService.recordSystemEvent(eventType, sessionId, PATH, ip, userId, meta);
    }
}
