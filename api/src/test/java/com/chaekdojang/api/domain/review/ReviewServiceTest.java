package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.review.dto.ReviewCreateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.review.dto.ReviewUpdateRequest;
import com.chaekdojang.api.domain.user.FollowRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookRepository bookRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;
    @Mock CommentRepository commentRepository;
    @Mock FollowRepository followRepository;

    @InjectMocks ReviewService reviewService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long REVIEW_ID = 10L;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication auth = mock(Authentication.class);
        // getOne 등 일부 메서드는 SecurityUtils를 호출하지 않으므로 lenient 처리
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getPrincipal()).thenReturn(USER_ID);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("독후감 작성 성공 — 책 없이도 작성 가능, likeCount·commentCount는 0")
    void create_withoutBook_success() {
        User author = stubUser(USER_ID);
        Review saved = stubReviewForResponse(REVIEW_ID, author);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(reviewRepository.save(any())).thenReturn(saved);

        ReviewResponse result = reviewService.create(new ReviewCreateRequest(null, "좋은 책이었어요", 5));

        assertThat(result.id()).isEqualTo(REVIEW_ID);
        assertThat(result.likeCount()).isZero();
        assertThat(result.commentCount()).isZero();
    }

    @Test
    @DisplayName("독후감 작성 — 존재하지 않는 책 ID → BOOK_NOT_FOUND")
    void create_bookNotFound_throws() {
        User author = stubUser(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(new ReviewCreateRequest(99L, "내용", 4)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_FOUND);
    }

    // ── getOne ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("독후감 단건 조회 성공 — likeCount·commentCount 포함")
    void getOne_success() {
        Review review = stubReviewForResponse(REVIEW_ID, stubUser(USER_ID));
        when(reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(3L);
        when(commentRepository.countByReviewIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(1L);

        ReviewResponse result = reviewService.getOne(REVIEW_ID);

        assertThat(result.likeCount()).isEqualTo(3L);
        assertThat(result.commentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("독후감 단건 조회 — 삭제된 독후감 → REVIEW_NOT_FOUND")
    void getOne_deletedOrMissing_throws() {
        when(reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getOne(REVIEW_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("독후감 수정 성공 — review.update() 호출됨")
    void update_success() {
        Review review = stubReviewForResponse(REVIEW_ID, stubUser(USER_ID));
        when(review.isAuthor(USER_ID)).thenReturn(true);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(0L);
        when(commentRepository.countByReviewIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(0L);

        reviewService.update(REVIEW_ID, new ReviewUpdateRequest("수정된 내용", 3));

        verify(review).update("수정된 내용", 3);
    }

    @Test
    @DisplayName("독후감 수정 — 다른 사용자가 시도 → FORBIDDEN, update() 미호출")
    void update_notAuthor_throws() {
        Review review = mock(Review.class);
        when(review.isAuthor(USER_ID)).thenReturn(false);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(REVIEW_ID, new ReviewUpdateRequest("수정", 3)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        verify(review, never()).update(any(), anyInt());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("독후감 삭제 성공 — softDelete() 호출됨")
    void delete_success() {
        Review review = mock(Review.class);
        when(review.isAuthor(USER_ID)).thenReturn(true);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        reviewService.delete(REVIEW_ID);

        verify(review).softDelete();
    }

    @Test
    @DisplayName("독후감 삭제 — 다른 사용자가 시도 → FORBIDDEN, softDelete() 미호출")
    void delete_notAuthor_throws() {
        Review review = mock(Review.class);
        when(review.isAuthor(USER_ID)).thenReturn(false);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(REVIEW_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        verify(review, never()).softDelete();
    }

    // ── getFeed ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("팔로잉 없으면 빈 피드 반환 — DB 조회 없음")
    void getFeed_noFollowing_returnsEmpty() {
        when(followRepository.findFollowingIdsByFollowerId(USER_ID)).thenReturn(List.of());

        List<ReviewResponse> result = reviewService.getFeed();

        assertThat(result).isEmpty();
        verifyNoInteractions(reviewRepository);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    // ReviewResponse.from()에서 사용되는 필드만 stub
    private User stubUser(Long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getNickname()).thenReturn("user" + id);
        lenient().when(user.getProfileImage()).thenReturn(null);
        return user;
    }

    // ReviewResponse.from() 호출이 필요한 테스트에서 사용
    private Review stubReviewForResponse(Long id, User author) {
        Review review = mock(Review.class);
        lenient().when(review.getId()).thenReturn(id);
        lenient().when(review.getAuthor()).thenReturn(author);
        lenient().when(review.getBook()).thenReturn(null);
        lenient().when(review.getContent()).thenReturn("테스트 내용");
        lenient().when(review.getRating()).thenReturn(5);
        lenient().when(review.getCreatedAt()).thenReturn(null);
        lenient().when(review.getUpdatedAt()).thenReturn(null);
        return review;
    }
}
