package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.chat.ChatBlockRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.library.LibraryStatus;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.notification.NotificationRepository;
import com.chaekdojang.api.domain.readinggoal.ReadingGoal;
import com.chaekdojang.api.domain.readinggoal.ReadingGoalRepository;
import com.chaekdojang.api.domain.review.ReviewBookmarkRepository;
import com.chaekdojang.api.domain.review.ReviewLikeRepository;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.subscription.SubscriptionRepository;
import com.chaekdojang.api.domain.user.dto.*;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ChatBlockRepository chatBlockRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewBookmarkRepository reviewBookmarkRepository;
    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final ReadingGoalRepository readingGoalRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final MetricEventRepository metricEventRepository;
    private final InquiryRepository inquiryRepository;
    private final MetricEventService metricEventService;

    public UserProfileResponse getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return buildProfile(userId);
    }

    @Transactional
    public void deleteMe() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findUser(userId);

        reviewLikeRepository.deleteAllByUserId(userId);
        reviewBookmarkRepository.deleteAllByUserId(userId);
        libraryRepository.deleteAllByUserId(userId);
        followRepository.deleteAllByFollowerIdOrFollowingId(userId, userId);
        notificationRepository.deleteAllByReceiverIdOrSenderId(userId, userId);
        subscriptionRepository.deleteAllByUserId(userId);
        userAuthProviderRepository.deleteAllByUserId(userId);
        metricEventRepository.anonymizeUser(userId);
        inquiryRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .forEach(inquiry -> inquiry.softDelete());

        user.anonymizeForDeletion("deleted-user-" + userId);
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findUser(userId);

        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }
        }

        user.updateProfile(request.nickname(), request.bio(), request.profileImage());
        metricEventService.recordCurrentRequestEvent(
                "profile_updated", userId, "/profile", Map.of("userId", userId));
        return buildProfile(userId);
    }

    @Transactional
    public UserProfileResponse updateReadingGoal(UpdateReadingGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findUser(userId);

        if (request.targetCount() == null) {
            if (user.getReadingGoalYear() != null) {
                readingGoalRepository.deleteByUserIdAndYear(userId, user.getReadingGoalYear());
            }
            user.updateReadingGoal(null, null);
            return buildProfile(userId);
        }

        int year = request.year() == null ? Year.now().getValue() : request.year();
        if (year < 2020 || year > 2100 || request.targetCount() < 1 || request.targetCount() > 999) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (user.getReadingGoalYear() != null && !user.getReadingGoalYear().equals(year)) {
            readingGoalRepository.deleteByUserIdAndYear(userId, user.getReadingGoalYear());
        }
        user.updateReadingGoal(year, request.targetCount());
        boolean publicVisible = request.publicVisible() == null || request.publicVisible();
        ReadingGoal goal = readingGoalRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> ReadingGoal.create(user, year, request.targetCount(), publicVisible));
        goal.update(request.targetCount(), publicVisible);
        readingGoalRepository.save(goal);
        return buildProfile(userId);
    }

    @Transactional
    public void setLifeBook(Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findUser(userId);
        if (bookId == null) {
            user.updateLifeBook(null);
        } else {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
            user.updateLifeBook(book);
        }
    }

    public UserProfileResponse getUserProfile(Long userId) {
        return buildProfile(userId);
    }

    public UserProfileResponse getUserProfileByNickname(String nickname) {
        User user = userRepository.findByNicknameIgnoreCaseAndDeletedAtIsNull(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return buildProfile(user.getId());
    }

    private UserProfileResponse buildProfile(Long userId) {
        User user = findUser(userId);
        long reviewCount = reviewRepository.countByAuthorIdAndDeletedAtIsNullAndHiddenFalse(userId);
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        UserProfileResponse.LibrarySummary librarySummary = new UserProfileResponse.LibrarySummary(
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.READING),
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.FINISHED),
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.WISHLIST)
        );
        long yearlyFinishedCount = user.getReadingGoalYear() == null
                ? 0
                : libraryRepository.countFinishedByUserIdAndYear(userId, user.getReadingGoalYear());
        boolean readingGoalPublicVisible = user.getReadingGoalYear() == null
                || readingGoalRepository.findByUserIdAndYear(userId, user.getReadingGoalYear())
                .map(ReadingGoal::isPublicVisible)
                .orElse(true);
        UserProfileResponse.ReadingGoalSummary readingGoal = UserProfileResponse.ReadingGoalSummary.of(
                user.getReadingGoalYear(),
                user.getReadingGoalCount(),
                yearlyFinishedCount,
                readingGoalPublicVisible
        );
        return UserProfileResponse.of(user, reviewCount, followerCount, followingCount, librarySummary, readingGoal);
    }

    public List<UserSummary> searchUsers(String q) {
        return userRepository.findByNicknameContainingIgnoreCaseAndDeletedAtIsNull(q)
                .stream()
                .map(UserSummary::from)
                .toList();
    }

    public List<UserRecommendationResponse> getRecommendations() {
        Long myId = SecurityUtils.getCurrentUserId();
        User me = findUser(myId);

        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(myId);
        List<Long> excludeIds = new ArrayList<>(followingIds);
        excludeIds.add(myId);
        excludeIds.addAll(chatBlockRepository.findBlockedUserIds(myId));

        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<Long, Integer> overlapMap = new HashMap<>();
        Map<Long, Integer> ratingMap = new HashMap<>();
        Set<Long> lifeBookMatchedIds = new HashSet<>();

        // 1. 공통으로 읽은 책: +1점/권
        List<Long> myBookIds = libraryRepository.findBookIdsByUserIdAndStatus(myId, LibraryStatus.FINISHED);
        if (!myBookIds.isEmpty()) {
            libraryRepository.findUsersWithMostBookOverlap(myBookIds, excludeIds)
                    .forEach(row -> {
                        Long userId = ((Number) row[0]).longValue();
                        int overlap = ((Number) row[1]).intValue();
                        overlapMap.put(userId, overlap);
                        scoreMap.merge(userId, overlap, Integer::sum);
                    });
        }

        // 2. 별점 유사도: 동일 +2점, 차이 1 +1점
        reviewRepository.findRatingSimilarity(myId, excludeIds)
                .forEach(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    int ratingScore = ((Number) row[1]).intValue();
                    ratingMap.put(userId, ratingScore);
                    scoreMap.merge(userId, ratingScore, Integer::sum);
                });

        // 3. 인생책 일치: +5점
        if (me.getLifeBook() != null) {
            userRepository.findAllByLifeBook_IdAndDeletedAtIsNull(me.getLifeBook().getId())
                    .forEach(user -> {
                        if (!excludeIds.contains(user.getId())) {
                            lifeBookMatchedIds.add(user.getId());
                            scoreMap.merge(user.getId(), 5, Integer::sum);
                        }
                    });
        }

        if (scoreMap.isEmpty()) return List.of();

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> userRepository.findById(entry.getKey())
                        .filter(user -> user.getDeletedAt() == null)
                        .map(user -> UserRecommendationResponse.from(
                                user,
                                overlapMap.getOrDefault(user.getId(), 0),
                                ratingMap.getOrDefault(user.getId(), 0),
                                lifeBookMatchedIds.contains(user.getId())))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public UserProfileResponse completeOnboarding(OnboardingRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = findUser(userId);
        user.completeOnboarding(normalizeGenres(request.genres()));
        return buildProfile(userId);
    }

    public List<UserRecommendationResponse> getOnboardingRecommendations() {
        Long myId = SecurityUtils.getCurrentUserId();
        User me = findUser(myId);
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(myId);
        List<Long> excludeIds = new ArrayList<>(followingIds);
        excludeIds.add(myId);
        excludeIds.addAll(chatBlockRepository.findBlockedUserIds(myId));

        List<AuthorActivityScore> authorScores = reviewRepository.findTopAuthorStatsByReviewCount(excludeIds)
                .stream()
                .limit(100)
                .map(row -> new AuthorActivityScore(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (java.time.LocalDateTime) row[2],
                        null
                ))
                .toList();
        if (authorScores.isEmpty()) return List.of();

        List<Long> candidateIds = authorScores.stream().map(AuthorActivityScore::userId).toList();

        Map<Long, java.time.LocalDateTime> lastActivityByUserId = metricEventRepository.findLastActivityByUserIds(candidateIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> (java.time.LocalDateTime) row[1]
                ));

        Map<Long, Integer> overlapByUserId = new HashMap<>();
        List<Long> myBookIds = libraryRepository.findBookIdsByUserIdAndStatus(myId, LibraryStatus.FINISHED);
        if (!myBookIds.isEmpty()) {
            libraryRepository.findUsersWithMostBookOverlap(myBookIds, excludeIds)
                    .forEach(row -> overlapByUserId.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue()));
        }

        Map<Long, Integer> ratingSimilarityByUserId = new HashMap<>();
        reviewRepository.findRatingSimilarity(myId, excludeIds)
                .forEach(row -> ratingSimilarityByUserId.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue()));

        List<Long> lifeBookMatchedIds = me.getLifeBook() == null
                ? List.of()
                : userRepository.findAllByLifeBook_IdAndDeletedAtIsNull(me.getLifeBook().getId())
                .stream()
                .map(User::getId)
                .toList();

        return authorScores.stream()
                .map(score -> score.withLastActivity(lastActivityByUserId.get(score.userId())))
                .map(score -> new OnboardingCandidate(
                        score,
                        onboardingRecommendationScore(
                                score,
                                overlapByUserId.getOrDefault(score.userId(), 0),
                                ratingSimilarityByUserId.getOrDefault(score.userId(), 0),
                                lifeBookMatchedIds.contains(score.userId())
                        )
                ))
                .sorted(Comparator
                        .comparingInt(OnboardingCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.author().reviewCount(), Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.author().lastActivity(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> candidate.author().lastReviewAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(candidate -> userRepository.findById(candidate.author().userId())
                        .filter(user -> user.getDeletedAt() == null)
                        .map(UserRecommendationResponse::from)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private int onboardingRecommendationScore(
            AuthorActivityScore authorScore,
            int commonBookCount,
            int ratingSimilarityScore,
            boolean lifeBookMatched
    ) {
        long reviewScore = Math.min(authorScore.reviewCount(), 50) * 10;
        int activityScore = recentActivityScore(authorScore.lastActivity());
        int lifeBookScore = lifeBookMatched ? 60 : 0;
        int commonBookScore = Math.min(commonBookCount, 10) * 15;
        int ratingScore = Math.min(ratingSimilarityScore, 20) * 8;
        return Math.toIntExact(reviewScore + activityScore + lifeBookScore + commonBookScore + ratingScore);
    }

    private int recentActivityScore(java.time.LocalDateTime lastActivity) {
        if (lastActivity == null) return 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        if (lastActivity.isAfter(now.minusDays(7))) return 40;
        if (lastActivity.isAfter(now.minusDays(30))) return 25;
        if (lastActivity.isAfter(now.minusDays(90))) return 10;
        return 5;
    }

    private record AuthorActivityScore(
            Long userId,
            long reviewCount,
            java.time.LocalDateTime lastReviewAt,
            java.time.LocalDateTime lastActivity
    ) {
        private AuthorActivityScore withLastActivity(java.time.LocalDateTime value) {
            return new AuthorActivityScore(userId, reviewCount, lastReviewAt, value);
        }
    }

    private record OnboardingCandidate(AuthorActivityScore author, int score) {
    }

    private String normalizeGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) return "";
        return genres.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.length() > 30 ? value.substring(0, 30) : value)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .limit(10)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public ReadingStatsResponse getReadingStats() {
        Long myId = SecurityUtils.getCurrentUserId();

        List<Object[]> monthlyData = libraryRepository.findMonthlyReadingStats(myId);
        List<ReadingStatsResponse.MonthlyCount> monthly = monthlyData.stream()
                .map(row -> new ReadingStatsResponse.MonthlyCount(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()
                ))
                .toList();

        List<Object[]> genreData = libraryRepository.findGenreStats(myId);
        List<ReadingStatsResponse.GenreCount> genres = genreData.stream()
                .map(row -> new ReadingStatsResponse.GenreCount(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .toList();

        int totalFinished = monthly.stream().mapToInt(ReadingStatsResponse.MonthlyCount::count).sum();

        return new ReadingStatsResponse(totalFinished, monthly, genres);
    }

    public ReadingReflectionResponse getReadingReflection() {
        Long myId = SecurityUtils.getCurrentUserId();
        List<Review> reviews = reviewRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(myId);
        int currentYear = Year.now().getValue();

        Map<YearMonth, Integer> monthlyCounts = new TreeMap<>();
        Map<Integer, Set<Long>> yearlyBookIds = new TreeMap<>();
        Map<String, Integer> genreByYear = new HashMap<>();
        Map<String, Integer> keywordCounts = new HashMap<>();
        Map<Long, List<Review>> reviewsByBook = new LinkedHashMap<>();
        int rereadCount = 0;

        for (Review review : reviews) {
            monthlyCounts.merge(YearMonth.from(review.getCreatedAt()), 1, Integer::sum);
            if (review.getPreviousReview() != null) rereadCount++;
            if (review.getBook() != null) {
                Book book = review.getBook();
                int year = review.getCreatedAt().getYear();
                yearlyBookIds.computeIfAbsent(year, ignored -> new HashSet<>()).add(book.getId());
                reviewsByBook.computeIfAbsent(book.getId(), ignored -> new ArrayList<>()).add(review);
                if (book.getCategory() != null && !book.getCategory().isBlank()) {
                    String genre = book.getCategory().trim();
                    genreByYear.merge(year + "\u0000" + genre, 1, Integer::sum);
                }
            }
            if (review.getKeywords() != null) {
                java.util.Arrays.stream(review.getKeywords().split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .forEach(value -> keywordCounts.merge(value, 1, Integer::sum));
            }
        }

        List<ReadingReflectionResponse.MonthlyReviewCount> monthly = monthlyCounts.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, Integer>comparingByKey().reversed())
                .limit(24)
                .map(entry -> new ReadingReflectionResponse.MonthlyReviewCount(
                        entry.getKey().getYear(), entry.getKey().getMonthValue(), entry.getValue()))
                .toList();
        List<ReadingReflectionResponse.YearlyBookCount> yearly = yearlyBookIds.entrySet().stream()
                .sorted(Map.Entry.<Integer, Set<Long>>comparingByKey().reversed())
                .map(entry -> new ReadingReflectionResponse.YearlyBookCount(entry.getKey(), entry.getValue().size()))
                .toList();
        List<ReadingReflectionResponse.GenreByYear> genres = genreByYear.entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\u0000", 2);
                    return new ReadingReflectionResponse.GenreByYear(
                            Integer.parseInt(key[0]), key[1], entry.getValue());
                })
                .sorted(Comparator.comparingInt(ReadingReflectionResponse.GenreByYear::year).reversed()
                        .thenComparing(ReadingReflectionResponse.GenreByYear::count, Comparator.reverseOrder()))
                .toList();
        List<ReadingReflectionResponse.KeywordCount> keywords = keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .map(entry -> new ReadingReflectionResponse.KeywordCount(entry.getKey(), entry.getValue()))
                .toList();

        List<ReadingReflectionResponse.RereadBook> rereadBooks = reviewsByBook.values().stream()
                .filter(values -> values.size() > 1)
                .map(values -> new ReadingReflectionResponse.RereadBook(
                        values.getFirst().getBook().getId(), values.getFirst().getBook().getTitle(), values.size(),
                        values.getFirst().getCreatedAt(), values.getLast().getCreatedAt()))
                .sorted(Comparator.comparing(ReadingReflectionResponse.RereadBook::latestAt).reversed())
                .toList();

        ReadingReflectionResponse.LongestRecordedBook longestBook = reviewsByBook.values().stream()
                .filter(values -> values.size() > 1)
                .map(values -> new ReadingReflectionResponse.LongestRecordedBook(
                        values.getFirst().getBook().getId(), values.getFirst().getBook().getTitle(),
                        ChronoUnit.DAYS.between(values.getFirst().getCreatedAt(), values.getLast().getCreatedAt()),
                        values.getFirst().getCreatedAt(), values.getLast().getCreatedAt()))
                .max(Comparator.comparingLong(ReadingReflectionResponse.LongestRecordedBook::days))
                .orElse(null);

        LocalDate memoryTarget = LocalDate.now().minusYears(1);
        List<ReadingReflectionResponse.MemoryReview> memories = reviews.stream()
                .filter(review -> Math.abs(ChronoUnit.DAYS.between(
                        memoryTarget, review.getCreatedAt().toLocalDate())) <= 7)
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .limit(5)
                .map(review -> new ReadingReflectionResponse.MemoryReview(
                        review.getId(), review.getBook() != null ? review.getBook().getId() : null,
                        review.getBook() != null ? review.getBook().getTitle() : "책 정보 없는 기록",
                        review.getRating(), review.getCreatedAt()))
                .toList();

        double averageInterval = averageReviewIntervalDays(reviews);
        int currentYearBookCount = yearlyBookIds.getOrDefault(currentYear, Set.of()).size();
        List<String> messages = buildReflectionMessages(
                currentYear, currentYearBookCount, genres, rereadBooks, memories, averageInterval);
        return new ReadingReflectionResponse(
                reviews.size(), rereadCount, currentYearBookCount, averageInterval,
                monthly, yearly, genres, keywords, rereadBooks, longestBook, memories, messages);
    }

    private double averageReviewIntervalDays(List<Review> reviews) {
        if (reviews.size() < 2) return 0;
        long totalDays = 0;
        for (int index = 1; index < reviews.size(); index++) {
            totalDays += Math.max(0, ChronoUnit.DAYS.between(
                    reviews.get(index - 1).getCreatedAt(), reviews.get(index).getCreatedAt()));
        }
        return Math.round((double) totalDays / (reviews.size() - 1) * 10.0) / 10.0;
    }

    private List<String> buildReflectionMessages(
            int currentYear,
            int currentYearBookCount,
            List<ReadingReflectionResponse.GenreByYear> genres,
            List<ReadingReflectionResponse.RereadBook> rereadBooks,
            List<ReadingReflectionResponse.MemoryReview> memories,
            double averageInterval) {
        List<String> messages = new ArrayList<>();
        if (currentYearBookCount > 0) {
            messages.add("올해는 " + currentYearBookCount + "권의 책에 기록을 남겼습니다.");
        }
        genres.stream().filter(item -> item.year() == currentYear).findFirst()
                .ifPresent(item -> messages.add("최근에는 " + item.genre() + " 분야의 기록이 가장 자주 남았습니다."));
        if (!rereadBooks.isEmpty()) {
            messages.add("다시 기록한 책이 " + rereadBooks.size() + "권 있습니다. 당시의 생각과 지금을 나란히 볼 수 있어요.");
        }
        if (!memories.isEmpty()) {
            messages.add("1년 전 이맘때 남긴 기록이 있습니다. 지금의 눈으로 다시 읽어보세요.");
        }
        if (averageInterval > 0) {
            messages.add("독후감은 평균 " + averageInterval + "일 간격으로 남겼습니다.");
        }
        if (messages.isEmpty()) {
            messages.add("첫 기록부터 천천히 쌓아가면 시간에 따른 생각의 흐름을 볼 수 있어요.");
        }
        return messages;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
