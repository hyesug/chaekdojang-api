package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.feedback.FeedbackProperties;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupAnalysisResponse;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryProperties;
import com.chaekdojang.api.domain.review.reflection.ReadingGroupAnalysisResult;
import com.chaekdojang.api.domain.review.reflection.ReviewReflectionAiClient;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReadingGroupAnalysisService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_REVIEWS = 12;

    private final ReadingGroupRepository groupRepository;
    private final ReadingGroupMemberRepository memberRepository;
    private final ReadingGroupBookRepository groupBookRepository;
    private final ReadingGroupReviewRepository groupReviewRepository;
    private final ReadingGroupBookAiAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final ReviewReflectionAiClient aiClient;
    private final ReviewAiSummaryProperties aiProperties;
    private final FeedbackProperties feedbackProperties;
    private final MetricEventRepository metricEventRepository;
    private final MetricEventService metricEventService;
    private final ObjectMapper objectMapper;

    public ReadingGroupAnalysisResponse find(
            ReadingGroupBook groupBook, List<Review> reviews) {
        return analysisRepository.findByGroupBookId(groupBook.getId())
                .map(value -> toResponse(value, sourceHash(reviews)))
                .orElse(null);
    }

    public boolean canManage(ReadingGroup group, Long userId) {
        return userId != null && (group.getOwner().getId().equals(userId)
                || memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(ReadingGroupMember::canManage).orElse(false))
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
    }

    @Transactional
    public ReadingGroupAnalysisResponse generate(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = groupRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!canManage(group, userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        validateEnabled();
        assertDailyLimit(userId);
        ReadingGroupBook groupBook = groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        List<Review> reviews = activeReviews(group, groupBook);
        if (reviews.size() < 2) throw new CustomException(ErrorCode.GROUP_ANALYSIS_NOT_ENOUGH_REVIEWS);
        List<Review> analyzedReviews = reviews.stream().limit(MAX_REVIEWS).toList();
        try {
            ReadingGroupAnalysisResult result = aiClient.analyzeReadingGroup(
                    groupBook.getBook().getTitle(), groupBook.getBook().getAuthor(),
                    analyzedReviews.stream().map(Review::getContent).toList());
            String currentHash = sourceHash(reviews);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            String json = objectMapper.writeValueAsString(result);
            ReadingGroupBookAiAnalysis analysis = analysisRepository.findByGroupBookId(groupBookId)
                    .orElseGet(() -> ReadingGroupBookAiAnalysis.create(
                            groupBook, user, json, currentHash, analyzedReviews.size(), aiProperties.getModel(),
                            ReviewReflectionAiClient.GROUP_ANALYSIS_PROMPT_VERSION,
                            result.inputTokens(), result.outputTokens()));
            if (analysis.getId() != null) {
                analysis.replace(user, json, currentHash, analyzedReviews.size(), aiProperties.getModel(),
                        ReviewReflectionAiClient.GROUP_ANALYSIS_PROMPT_VERSION,
                        result.inputTokens(), result.outputTokens());
            }
            ReadingGroupBookAiAnalysis saved = analysisRepository.save(analysis);
            recordSuccess(group, groupBook, userId, analyzedReviews.size(), result);
            return toResponse(saved, currentHash);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("reading group analysis failed: groupBookId={}", groupBookId, e);
            metricEventService.recordCurrentRequestEvent(
                    "reading_group_ai_analysis_failed", userId, groupBookPath(group, groupBook),
                    Map.of("groupId", group.getId(), "groupBookId", groupBookId,
                            "reason", e.getClass().getSimpleName()));
            throw new CustomException(ErrorCode.GROUP_ANALYSIS_AI_FAILED);
        }
    }

    private List<Review> activeReviews(ReadingGroup group, ReadingGroupBook groupBook) {
        return groupReviewRepository.findAllByGroupBookIdOrderByCreatedAtDesc(groupBook.getId()).stream()
                .map(ReadingGroupReview::getReview)
                .filter(review -> review.getDeletedAt() == null
                        && (group.getVisibility() == ReadingGroupVisibility.PRIVATE || !review.isHidden()))
                .toList();
    }

    private ReadingGroupAnalysisResponse toResponse(
            ReadingGroupBookAiAnalysis value, String currentHash) {
        try {
            ReadingGroupAnalysisResult result = objectMapper.readValue(
                    value.getAnalysisJson(), ReadingGroupAnalysisResult.class);
            return new ReadingGroupAnalysisResponse(
                    result.summary(), result.commonThoughts(), result.differentInterpretations(),
                    result.keyThemes(), result.emotions(), result.discussionQuestions(),
                    value.getReviewCount(), !value.getSourceHash().equals(currentHash),
                    value.getModel(), value.getPromptVersion(), value.getUpdatedAt());
        } catch (Exception e) {
            log.warn("stored reading group analysis parsing failed: id={}", value.getId(), e);
            return null;
        }
    }

    private String sourceHash(List<Review> reviews) {
        String source = reviews.stream()
                .map(review -> review.getId() + ":" + review.getUpdatedAt() + ":" + review.getContent())
                .collect(java.util.stream.Collectors.joining("\n---\n"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void validateEnabled() {
        if (!aiProperties.isEnabled() || aiProperties.getApiKey() == null
                || aiProperties.getApiKey().isBlank()) {
            throw new CustomException(ErrorCode.GROUP_ANALYSIS_AI_DISABLED);
        }
    }

    private void assertDailyLimit(Long userId) {
        int limit = feedbackProperties.getMemberDailyLimit();
        if (limit <= 0) return;
        var todayStart = LocalDate.now(KST).atStartOfDay();
        long used = List.of("feedback_succeeded", "reflection_ai_succeeded",
                        "reading_group_ai_question_succeeded", "reading_group_ai_analysis_succeeded")
                .stream().mapToLong(eventType -> metricEventRepository
                        .countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(userId, eventType, todayStart))
                .sum();
        if (used >= limit) throw new CustomException(ErrorCode.GROUP_ANALYSIS_AI_LIMIT_EXCEEDED);
    }

    private void recordSuccess(
            ReadingGroup group, ReadingGroupBook groupBook, Long userId, int reviewCount,
            ReadingGroupAnalysisResult result) {
        metricEventService.recordCurrentRequestEvent(
                "reading_group_ai_analysis_succeeded", userId, groupBookPath(group, groupBook),
                Map.of("groupId", group.getId(), "groupBookId", groupBook.getId(),
                        "reviewCount", reviewCount, "model", aiProperties.getModel(),
                        "promptVersion", ReviewReflectionAiClient.GROUP_ANALYSIS_PROMPT_VERSION,
                        "inputTokens", result.inputTokens(), "outputTokens", result.outputTokens(),
                        "estimatedCostUsd", estimatedCostUsd(result.inputTokens(), result.outputTokens())));
    }

    private String groupBookPath(ReadingGroup group, ReadingGroupBook groupBook) {
        return "/groups/" + group.getSlug() + "/books/" + groupBook.getId() + "/result";
    }

    private double estimatedCostUsd(long inputTokens, long outputTokens) {
        double cost = inputTokens * aiProperties.getInputCostPerMillionTokens() / 1_000_000d
                + outputTokens * aiProperties.getOutputCostPerMillionTokens() / 1_000_000d;
        return Math.round(cost * 100_000_000d) / 100_000_000d;
    }
}
