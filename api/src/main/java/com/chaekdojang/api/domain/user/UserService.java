package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.library.LibraryStatus;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.notification.NotificationRepository;
import com.chaekdojang.api.domain.review.ReviewBookmarkRepository;
import com.chaekdojang.api.domain.review.ReviewLikeRepository;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.subscription.SubscriptionRepository;
import com.chaekdojang.api.domain.user.dto.*;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewBookmarkRepository reviewBookmarkRepository;
    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final MetricEventRepository metricEventRepository;
    private final InquiryRepository inquiryRepository;

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
        long reviewCount = reviewRepository.countByAuthorIdAndDeletedAtIsNull(userId);
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        UserProfileResponse.LibrarySummary librarySummary = new UserProfileResponse.LibrarySummary(
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.READING),
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.FINISHED),
                libraryRepository.countByUserIdAndStatus(userId, LibraryStatus.WISHLIST)
        );
        return UserProfileResponse.of(user, reviewCount, followerCount, followingCount, librarySummary);
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

        Map<Long, Integer> scoreMap = new HashMap<>();

        // 1. 공통으로 읽은 책: +1점/권
        List<Long> myBookIds = libraryRepository.findBookIdsByUserId(myId);
        if (!myBookIds.isEmpty()) {
            libraryRepository.findUsersWithMostBookOverlap(myBookIds, excludeIds)
                    .forEach(row -> {
                        Long userId = ((Number) row[0]).longValue();
                        int overlap = ((Number) row[1]).intValue();
                        scoreMap.merge(userId, overlap, Integer::sum);
                    });
        }

        // 2. 별점 유사도: 동일 +2점, 차이 1 +1점
        reviewRepository.findRatingSimilarity(myId, excludeIds)
                .forEach(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    int ratingScore = ((Number) row[1]).intValue();
                    scoreMap.merge(userId, ratingScore, Integer::sum);
                });

        // 3. 인생책 일치: +5점
        if (me.getLifeBook() != null) {
            userRepository.findAllByLifeBook_IdAndDeletedAtIsNull(me.getLifeBook().getId())
                    .forEach(user -> {
                        if (!excludeIds.contains(user.getId())) {
                            scoreMap.merge(user.getId(), 5, Integer::sum);
                        }
                    });
        }

        if (scoreMap.isEmpty()) return List.of();

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> userRepository.findById(entry.getKey())
                        .map(user -> UserRecommendationResponse.from(user, entry.getValue()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
