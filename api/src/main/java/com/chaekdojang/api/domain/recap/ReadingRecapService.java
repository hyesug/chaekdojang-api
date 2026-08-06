package com.chaekdojang.api.domain.recap;

import com.chaekdojang.api.domain.recap.dto.*;
import com.chaekdojang.api.domain.review.*;
import com.chaekdojang.api.domain.user.FollowRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingRecapService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReadingRecapPreferenceRepository preferenceRepository;
    private final ReadingRecapIssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewBookmarkRepository bookmarkRepository;
    private final FollowRepository followRepository;

    public ReadingRecapPreferenceResponse getPreference() {
        Long userId = SecurityUtils.getCurrentUserId();
        return preferenceRepository.findByUserId(userId)
                .map(ReadingRecapPreferenceResponse::from)
                .orElse(null);
    }

    @Transactional
    public ReadingRecapPreferenceResponse subscribe(ReadingRecapPreferenceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ReadingRecapPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> ReadingRecapPreference.create(
                        user, request.frequency(), request.shouldIncludeFollowing(), request.shouldIncludeMemories()));
        preference.update(request.frequency(), request.shouldIncludeFollowing(), request.shouldIncludeMemories());
        return ReadingRecapPreferenceResponse.from(preferenceRepository.saveAndFlush(preference));
    }

    @Transactional
    public void unsubscribe() {
        Long userId = SecurityUtils.getCurrentUserId();
        preferenceRepository.findByUserId(userId).ifPresent(ReadingRecapPreference::disable);
    }

    public List<ReadingRecapIssueResponse> getIssues() {
        Long userId = SecurityUtils.getCurrentUserId();
        return issueRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(24)
                .map(ReadingRecapIssueResponse::from)
                .toList();
    }

    @Transactional
    public ReadingRecapIssueResponse generateCurrentIssue() {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingRecapPreference preference = preferenceRepository.findByUserId(userId)
                .filter(ReadingRecapPreference::isEnabled)
                .orElse(null);
        if (preference == null) return null;
        ReadingRecapIssue issue = generate(preference, LocalDate.now(KST));
        return issue != null ? ReadingRecapIssueResponse.from(issue) : null;
    }

    @Transactional
    public ReadingRecapIssueResponse markRead(Long issueId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingRecapIssue issue = issueRepository.findByIdAndUserId(issueId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
        issue.markRead();
        return ReadingRecapIssueResponse.from(issue);
    }

    @Transactional
    public void generateDueIssues() {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime now = LocalDateTime.now(KST);
        for (ReadingRecapPreference preference : preferenceRepository.findAllByEnabledTrue()) {
            if (preference.getUser().getDeletedAt() != null || !isDue(preference, now)) continue;
            generate(preference, today);
        }
    }

    private ReadingRecapIssue generate(ReadingRecapPreference preference, LocalDate today) {
        PeriodWindow period = periodWindow(preference.getFrequency(), today);
        ReadingRecapIssue existing = issueRepository
                .findByUserIdAndPeriodKey(preference.getUser().getId(), period.key())
                .orElse(null);
        if (existing != null) {
            preference.markDelivered();
            return existing;
        }

        LocalDateTime start = period.start().atStartOfDay();
        LocalDateTime endExclusive = period.end().plusDays(1).atStartOfDay();
        Long userId = preference.getUser().getId();
        List<Review> ownReviews = reviewRepository
                .findAllByAuthorIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, endExclusive);
        List<Long> followingIds = preference.isIncludeFollowing()
                ? followRepository.findFollowingIdsByFollowerId(userId)
                : List.of();
        List<Review> followingReviews = followingIds.isEmpty()
                ? List.of()
                : reviewRepository.findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
                        followingIds, start, endExclusive);
        List<ReviewBookmark> bookmarks = bookmarkRepository
                .findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, endExclusive);
        List<Review> continuations = reviewRepository
                .findAllBySourceReview_Author_IdAndDeletedAtIsNullAndHiddenFalseAndCreatedAtBetweenOrderByCreatedAtDesc(
                        userId, start, endExclusive);
        Review memory = null;
        if (preference.isIncludeMemories()) {
            List<Review> memories = reviewRepository
                    .findAllByAuthorIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
                            userId, start.minusYears(1), endExclusive.minusYears(1));
            if (!memories.isEmpty()) memory = memories.getFirst();
        }
        if (ownReviews.isEmpty() && followingReviews.isEmpty() && bookmarks.isEmpty()
                && continuations.isEmpty() && memory == null) {
            return null;
        }

        List<String> summaryParts = new ArrayList<>();
        if (!ownReviews.isEmpty()) summaryParts.add("내 독후감 " + ownReviews.size() + "개");
        if (!followingReviews.isEmpty()) summaryParts.add("팔로우한 독자의 새 기록 " + followingReviews.size() + "개");
        if (!bookmarks.isEmpty()) summaryParts.add("저장한 독후감 " + bookmarks.size() + "개");
        if (!continuations.isEmpty()) summaryParts.add("내 글에서 이어진 독후감 " + continuations.size() + "개");
        if (memory != null) summaryParts.add("1년 전 기록 1개");
        Review featured = !ownReviews.isEmpty() ? ownReviews.getFirst()
                : !followingReviews.isEmpty() ? followingReviews.getFirst() : null;
        String title = preference.getFrequency() == ReadingRecapFrequency.WEEKLY
                ? "이번 주 기록 회고" : "이번 달 기록 회고";
        ReadingRecapIssue issue = ReadingRecapIssue.create(
                preference.getUser(), period.key(), period.start(), period.end(), title,
                String.join(" · ", summaryParts), ownReviews.size(), followingReviews.size(),
                bookmarks.size(), continuations.size(), featured, memory,
                memory != null && memory.getBook() != null ? memory.getBook().getTitle() : null);
        ReadingRecapIssue saved = issueRepository.saveAndFlush(issue);
        preference.markDelivered();
        return saved;
    }

    private boolean isDue(ReadingRecapPreference preference, LocalDateTime now) {
        if (preference.getLastDeliveredAt() == null) return true;
        int days = preference.getFrequency() == ReadingRecapFrequency.WEEKLY ? 7 : 28;
        return !preference.getLastDeliveredAt().plusDays(days).isAfter(now);
    }

    private PeriodWindow periodWindow(ReadingRecapFrequency frequency, LocalDate today) {
        if (frequency == ReadingRecapFrequency.MONTHLY) {
            LocalDate start = today.withDayOfMonth(1);
            return new PeriodWindow("MONTHLY:" + YearMonth.from(today), start,
                    today.with(TemporalAdjusters.lastDayOfMonth()));
        }
        WeekFields fields = WeekFields.ISO;
        LocalDate start = today.with(fields.dayOfWeek(), 1);
        String key = "WEEKLY:" + today.get(fields.weekBasedYear()) + "-W"
                + String.format("%02d", today.get(fields.weekOfWeekBasedYear()));
        return new PeriodWindow(key, start, start.plusDays(6));
    }

    private record PeriodWindow(String key, LocalDate start, LocalDate end) {}
}
