package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewBookmarkService {

    private final ReviewBookmarkRepository bookmarkRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository likeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void bookmark(Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (bookmarkRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new CustomException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
        Review review = reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        bookmarkRepository.save(ReviewBookmark.builder().review(review).user(user).build());
    }

    @Transactional
    public void unbookmark(Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!bookmarkRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new CustomException(ErrorCode.BOOKMARK_NOT_FOUND);
        }
        bookmarkRepository.deleteByReviewIdAndUserId(reviewId, userId);
    }

    public boolean isBookmarked(Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return bookmarkRepository.existsByReviewIdAndUserId(reviewId, userId);
    }

    public List<ReviewResponse> getMyBookmarks() {
        Long userId = SecurityUtils.getCurrentUserId();
        return bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(b -> !b.getReview().isHidden() && b.getReview().getDeletedAt() == null)
                .limit(100)
                .map(b -> ReviewResponse.from(
                        b.getReview(),
                        likeRepository.countByReviewId(b.getReview().getId()),
                        0L
                ))
                .toList();
    }
}
