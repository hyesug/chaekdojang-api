package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.review.dto.CommentCreateRequest;
import com.chaekdojang.api.domain.review.dto.CommentResponse;
import com.chaekdojang.api.domain.review.dto.CommentUpdateRequest;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse create(Long reviewId, CommentCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Comment comment = Comment.builder()
                .review(review)
                .author(author)
                .content(request.content())
                .build();
        Comment saved = commentRepository.save(comment);
        notificationService.send(review.getAuthor(), author, NotificationType.COMMENT, reviewId);
        return CommentResponse.from(saved);
    }

    public List<CommentResponse> getAll(Long reviewId) {
        reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        return commentRepository.findAllByReviewIdAndDeletedAtIsNullOrderByCreatedAtAsc(reviewId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse update(Long reviewId, Long commentId, CommentUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getReview().getId().equals(reviewId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        comment.update(request.content());
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long reviewId, Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getReview().getId().equals(reviewId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        comment.softDelete();
    }
}
