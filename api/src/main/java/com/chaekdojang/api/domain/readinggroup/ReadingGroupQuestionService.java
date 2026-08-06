package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.feedback.FeedbackProperties;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.readinggroup.dto.*;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryProperties;
import com.chaekdojang.api.domain.review.reflection.ReadingGroupQuestionResult;
import com.chaekdojang.api.domain.review.reflection.ReviewReflectionAiClient;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReadingGroupQuestionService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReadingGroupRepository groupRepository;
    private final ReadingGroupMemberRepository memberRepository;
    private final ReadingGroupBookRepository groupBookRepository;
    private final ReadingGroupQuestionRepository questionRepository;
    private final ReadingGroupQuestionResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final ReviewReflectionAiClient aiClient;
    private final ReviewAiSummaryProperties aiProperties;
    private final FeedbackProperties feedbackProperties;
    private final MetricEventRepository metricEventRepository;
    private final MetricEventService metricEventService;

    public ReadingGroupQuestionListResponse getQuestions(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        ReadingGroup group = findGroup(slug);
        boolean canManage = canManage(group, userId);
        if (!canRead(group, userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        ReadingGroupBook groupBook = findGroupBook(group, groupBookId);

        List<ReadingGroupQuestion> questions = questionRepository
                .findAllByGroupBookIdOrderByCreatedAtAsc(groupBook.getId())
                .stream()
                .filter(value -> value.isPublished() || canManage)
                .toList();
        List<Long> questionIds = questions.stream().map(ReadingGroupQuestion::getId).toList();
        Set<Long> visibleMemberIds = visibleMemberIds(group);
        Map<Long, List<ReadingGroupQuestionAnswerResponse>> answers = questionIds.isEmpty()
                ? Map.of()
                : responseRepository.findAllByQuestionIdInOrderByCreatedAtAsc(questionIds)
                .stream()
                .filter(value -> value.getUser().getDeletedAt() == null)
                .filter(value -> visibleMemberIds.contains(value.getUser().getId()))
                .collect(Collectors.groupingBy(
                        value -> value.getQuestion().getId(),
                        Collectors.mapping(
                                value -> ReadingGroupQuestionAnswerResponse.from(value, userId),
                                Collectors.toList())));
        return new ReadingGroupQuestionListResponse(
                canManage,
                isApprovedMember(group, userId) && groupBook.getStatus() != ReadingGroupBookStatus.COMPLETED,
                canManage && groupBook.getStatus() != ReadingGroupBookStatus.COMPLETED,
                questions.stream()
                        .map(value -> ReadingGroupQuestionItemResponse.of(
                                value, answers.getOrDefault(value.getId(), List.of())))
                        .toList());
    }

    @Transactional
    public ReadingGroupQuestionItemResponse createQuestion(
            String slug, Long groupBookId, ReadingGroupQuestionCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        assertManager(group, userId);
        ReadingGroupBook groupBook = findGroupBook(group, groupBookId);
        assertQuestionOpen(groupBook);
        User user = findUser(userId);
        ReadingGroupQuestion saved = questionRepository.saveAndFlush(
                ReadingGroupQuestion.manual(groupBook, user, request.question().trim()));
        return ReadingGroupQuestionItemResponse.of(saved, List.of());
    }

    @Transactional
    public ReadingGroupQuestionItemResponse generateAiDraft(String slug, Long groupBookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        assertManager(group, userId);
        ReadingGroupBook groupBook = findGroupBook(group, groupBookId);
        assertQuestionOpen(groupBook);
        validateAiEnabled();
        assertDailyLimit(userId);
        try {
            List<String> existing = questionRepository
                    .findAllByGroupBookIdOrderByCreatedAtAsc(groupBook.getId())
                    .stream().map(ReadingGroupQuestion::getQuestion).toList();
            ReadingGroupQuestionResult result = aiClient.createReadingGroupQuestion(
                    groupBook.getBook().getTitle(), groupBook.getBook().getAuthor(), existing);
            ReadingGroupQuestion saved = questionRepository.saveAndFlush(
                    ReadingGroupQuestion.aiDraft(
                            groupBook, findUser(userId), result.question(), aiProperties.getModel(),
                            ReviewReflectionAiClient.GROUP_QUESTION_PROMPT_VERSION));
            metricEventService.recordCurrentRequestEvent(
                    "reading_group_ai_question_succeeded", userId, groupBookPath(group, groupBook),
                    Map.of("groupId", group.getId(), "groupBookId", groupBook.getId(),
                            "questionId", saved.getId(), "model", aiProperties.getModel(),
                            "promptVersion", ReviewReflectionAiClient.GROUP_QUESTION_PROMPT_VERSION,
                            "inputTokens", result.inputTokens(), "outputTokens", result.outputTokens(),
                            "estimatedCostUsd", estimatedCostUsd(result.inputTokens(), result.outputTokens())));
            return ReadingGroupQuestionItemResponse.of(saved, List.of());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("reading group question generation failed: groupBookId={}", groupBookId, e);
            metricEventService.recordCurrentRequestEvent(
                    "reading_group_ai_question_failed", userId, groupBookPath(group, groupBook),
                    Map.of("groupId", group.getId(), "groupBookId", groupBook.getId(),
                            "reason", e.getClass().getSimpleName()));
            throw new CustomException(ErrorCode.GROUP_QUESTION_AI_FAILED);
        }
    }

    @Transactional
    public ReadingGroupQuestionItemResponse updateQuestion(
            String slug, Long groupBookId, Long questionId, ReadingGroupQuestionUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        assertManager(group, userId);
        ReadingGroupQuestion question = findQuestion(findGroupBook(group, groupBookId), questionId);
        question.edit(request.question().trim());
        return ReadingGroupQuestionItemResponse.of(question, answers(question, userId, group));
    }

    @Transactional
    public ReadingGroupQuestionItemResponse publishQuestion(
            String slug, Long groupBookId, Long questionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        assertManager(group, userId);
        ReadingGroupBook groupBook = findGroupBook(group, groupBookId);
        assertQuestionOpen(groupBook);
        ReadingGroupQuestion question = findQuestion(groupBook, questionId);
        question.publish();
        return ReadingGroupQuestionItemResponse.of(question, answers(question, userId, group));
    }

    @Transactional
    public void deleteQuestion(String slug, Long groupBookId, Long questionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        assertManager(group, userId);
        ReadingGroupQuestion question = findQuestion(findGroupBook(group, groupBookId), questionId);
        questionRepository.delete(question);
    }

    @Transactional
    public ReadingGroupQuestionAnswerResponse saveAnswer(
            String slug, Long groupBookId, Long questionId, ReadingGroupQuestionAnswerRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        if (!isApprovedMember(group, userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        ReadingGroupBook groupBook = findGroupBook(group, groupBookId);
        assertQuestionOpen(groupBook);
        ReadingGroupQuestion question = findQuestion(groupBook, questionId);
        if (!question.isPublished()) throw new CustomException(ErrorCode.GROUP_QUESTION_NOT_PUBLISHED);
        ReadingGroupQuestionResponse answer = responseRepository
                .findByQuestionIdAndUserId(questionId, userId)
                .orElseGet(() -> ReadingGroupQuestionResponse.create(question, findUser(userId), request.content().trim()));
        answer.edit(request.content().trim());
        ReadingGroupQuestionResponse saved = responseRepository.saveAndFlush(answer);
        return ReadingGroupQuestionAnswerResponse.from(saved, userId);
    }

    @Transactional
    public void deleteAnswer(String slug, Long groupBookId, Long questionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = findGroup(slug);
        if (!isApprovedMember(group, userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        ReadingGroupQuestion question = findQuestion(findGroupBook(group, groupBookId), questionId);
        ReadingGroupQuestionResponse answer = responseRepository.findByQuestionIdAndUserId(question.getId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_QUESTION_RESPONSE_NOT_FOUND));
        responseRepository.delete(answer);
    }

    private List<ReadingGroupQuestionAnswerResponse> answers(
            ReadingGroupQuestion question, Long userId, ReadingGroup group) {
        Set<Long> visibleMemberIds = visibleMemberIds(group);
        return responseRepository.findAllByQuestionIdInOrderByCreatedAtAsc(List.of(question.getId()))
                .stream()
                .filter(value -> value.getUser().getDeletedAt() == null)
                .filter(value -> visibleMemberIds.contains(value.getUser().getId()))
                .map(value -> ReadingGroupQuestionAnswerResponse.from(value, userId)).toList();
    }

    private Set<Long> visibleMemberIds(ReadingGroup group) {
        java.util.HashSet<Long> ids = memberRepository
                .findAllByGroupIdAndStatusOrderByCreatedAtAsc(
                        group.getId(), ReadingGroupMemberStatus.APPROVED)
                .stream().map(value -> value.getUser().getId())
                .collect(Collectors.toCollection(java.util.HashSet::new));
        ids.add(group.getOwner().getId());
        return ids;
    }

    private ReadingGroup findGroup(String slug) {
        return groupRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private ReadingGroupBook findGroupBook(ReadingGroup group, Long groupBookId) {
        return groupBookRepository.findByIdAndGroupId(groupBookId, group.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private ReadingGroupQuestion findQuestion(ReadingGroupBook groupBook, Long questionId) {
        return questionRepository.findByIdAndGroupBookId(questionId, groupBook.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_QUESTION_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean canRead(ReadingGroup group, Long userId) {
        return group.getVisibility() == ReadingGroupVisibility.PUBLIC
                || isApprovedMember(group, userId)
                || canManage(group, userId)
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
    }

    private boolean isApprovedMember(ReadingGroup group, Long userId) {
        return userId != null && (group.getOwner().getId().equals(userId)
                || memberRepository.existsByGroupIdAndUserIdAndStatus(
                        group.getId(), userId, ReadingGroupMemberStatus.APPROVED));
    }

    private boolean canManage(ReadingGroup group, Long userId) {
        return userId != null && (group.getOwner().getId().equals(userId)
                || memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(ReadingGroupMember::canManage).orElse(false))
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
    }

    private void assertManager(ReadingGroup group, Long userId) {
        if (!canManage(group, userId)) throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void validateAiEnabled() {
        if (!aiProperties.isEnabled()
                || aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new CustomException(ErrorCode.GROUP_QUESTION_AI_DISABLED);
        }
    }

    private void assertQuestionOpen(ReadingGroupBook groupBook) {
        if (groupBook.getStatus() == ReadingGroupBookStatus.COMPLETED) {
            throw new CustomException(ErrorCode.GROUP_BOOK_ARCHIVED);
        }
    }

    private void assertDailyLimit(Long userId) {
        int limit = feedbackProperties.getMemberDailyLimit();
        if (limit <= 0) return;
        var todayStart = LocalDate.now(KST).atStartOfDay();
        long used = metricEventRepository.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(
                userId, "feedback_succeeded", todayStart)
                + metricEventRepository.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(
                userId, "reflection_ai_succeeded", todayStart)
                + metricEventRepository.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(
                userId, "reading_group_ai_question_succeeded", todayStart);
        if (used >= limit) throw new CustomException(ErrorCode.GROUP_QUESTION_AI_LIMIT_EXCEEDED);
    }

    private String groupBookPath(ReadingGroup group, ReadingGroupBook groupBook) {
        return "/groups/" + group.getSlug() + "/books/" + groupBook.getId();
    }

    private double estimatedCostUsd(long inputTokens, long outputTokens) {
        double cost = inputTokens * aiProperties.getInputCostPerMillionTokens() / 1_000_000d
                + outputTokens * aiProperties.getOutputCostPerMillionTokens() / 1_000_000d;
        return Math.round(cost * 100_000_000d) / 100_000_000d;
    }
}
