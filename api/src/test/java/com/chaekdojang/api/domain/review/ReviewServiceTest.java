package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.chat.ChatBlockRepository;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.review.dto.ReviewCreateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewRereadHistoryResponse;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.review.dto.ReviewUpdateRequest;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryRepository;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryService;
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
import java.time.LocalDateTime;

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
    @Mock ChatBlockRepository chatBlockRepository;
    @Mock LibraryRepository libraryRepository;
    @Mock NotificationService notificationService;
    @Mock ReviewAiSummaryRepository reviewAiSummaryRepository;
    @Mock ReviewAiSummaryService reviewAiSummaryService;
    @Mock MetricEventService metricEventService;

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

        ReviewResponse result = reviewService.create(new ReviewCreateRequest(null, "좋은 책이었어요", 5, false, null, null, null, null, null));

        assertThat(result.id()).isEqualTo(REVIEW_ID);
        assertThat(result.likeCount()).isZero();
        assertThat(result.commentCount()).isZero();
        verify(metricEventService).recordCurrentRequestEvent(
                eq("review_created"), eq(USER_ID), eq("/reviews/" + REVIEW_ID),
                argThat(meta -> REVIEW_ID.equals(meta.get("reviewId"))
                        && !meta.containsKey("content")
                        && !meta.containsKey("body")));
    }

    @Test
    @DisplayName("독후감 작성 — 존재하지 않는 책 ID → BOOK_NOT_FOUND")
    void create_bookNotFound_throws() {
        User author = stubUser(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(new ReviewCreateRequest(99L, "내용", 4, false, null, null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("재독 독후감 작성 — 본인의 같은 책 이전 기록을 새 글에 연결")
    void create_reread_success() {
        User author = stubUser(USER_ID);
        Book book = mock(Book.class);
        Review previous = stubReviewForResponse(REVIEW_ID, author);
        Review saved = stubReviewForResponse(11L, author);
        when(book.getId()).thenReturn(7L);
        when(book.getSource()).thenReturn(BookSource.KAKAO);
        when(previous.isAuthor(USER_ID)).thenReturn(true);
        when(previous.getBook()).thenReturn(book);
        when(saved.getBook()).thenReturn(book);
        when(saved.getPreviousReview()).thenReturn(previous);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(previous));
        when(reviewRepository.existsByPreviousReviewIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenReturn(saved);

        ReviewResponse result = reviewService.create(
                new ReviewCreateRequest(7L, "다시 읽고 남긴 생각", 4, false, false, REVIEW_ID, null, null, null));

        assertThat(result.previousReviewId()).isEqualTo(REVIEW_ID);
        verify(reviewRepository).saveAndFlush(argThat(review -> review.getPreviousReview() == previous));
        verify(metricEventService).recordCurrentRequestEvent(
                eq("review_created"), eq(USER_ID), eq("/reviews/11"),
                argThat(meta -> REVIEW_ID.equals(meta.get("previousReviewId"))));
    }

    @Test
    @DisplayName("재독 독후감 작성 — 다른 사람 글은 연결할 수 없음")
    void create_rereadFromOtherUser_throws() {
        User author = stubUser(USER_ID);
        Book book = mock(Book.class);
        Review previous = stubReviewForResponse(REVIEW_ID, stubUser(OTHER_ID));
        when(previous.isAuthor(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> reviewService.create(
                new ReviewCreateRequest(7L, "내용", 4, false, false, REVIEW_ID, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REREAD_SOURCE);
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("재독 독후감 작성 — 이미 다음 기록이 있으면 중복 연결을 차단")
    void create_duplicateReread_throws() {
        User author = stubUser(USER_ID);
        Book book = mock(Book.class);
        Review previous = stubReviewForResponse(REVIEW_ID, author);
        when(book.getId()).thenReturn(7L);
        when(previous.isAuthor(USER_ID)).thenReturn(true);
        when(previous.getBook()).thenReturn(book);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(previous));
        when(reviewRepository.existsByPreviousReviewIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(
                new ReviewCreateRequest(7L, "내용", 4, false, false, REVIEW_ID, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REREAD_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("재독 독후감 작성 — 삭제된 중간 기록 뒤에 활성 기록이 남아 있으면 과거에서 분기할 수 없음")
    void create_rereadFromAncestorWithActiveDescendant_throws() {
        User author = stubUser(USER_ID);
        Book book = mock(Book.class);
        Review first = historyReview(10L, author, book, null, LocalDateTime.of(2024, 1, 1, 10, 0), false);
        Review deletedMiddle = historyReview(11L, author, book, first, LocalDateTime.of(2025, 1, 1, 10, 0), false);
        Review latest = historyReview(12L, author, book, deletedMiddle, LocalDateTime.of(2026, 1, 1, 10, 0), false);
        when(book.getId()).thenReturn(7L);
        when(first.isAuthor(USER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(author));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(reviewRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(first));
        when(reviewRepository.existsByPreviousReviewIdAndDeletedAtIsNull(10L)).thenReturn(false);
        when(reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(USER_ID, 7L))
                .thenReturn(List.of(first, latest));

        assertThatThrownBy(() -> reviewService.create(
                new ReviewCreateRequest(7L, "내용", 4, false, false, 10L, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REREAD_ALREADY_EXISTS);
    }

    // ── getOne ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("독후감 단건 조회 성공 — likeCount·commentCount 포함")
    void getOne_success() {
        Review review = stubReviewForResponse(REVIEW_ID, stubUser(USER_ID));
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));
        when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(3L);
        when(commentRepository.countByReviewIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(1L);

        ReviewResponse result = reviewService.getOne(REVIEW_ID);

        assertThat(result.likeCount()).isEqualTo(3L);
        assertThat(result.commentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("독후감 단건 조회 — 삭제된 독후감 → REVIEW_NOT_FOUND")
    void getOne_deletedOrMissing_throws() {
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getOne(REVIEW_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("독후감 단건 조회 — 비공개 독후감은 작성자만 조회 가능")
    void getOne_hidden_allowsAuthorOnly() {
        Review review = stubReviewForResponse(REVIEW_ID, stubUser(USER_ID));
        when(review.isHidden()).thenReturn(true);
        when(review.isAuthor(USER_ID)).thenReturn(true);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        ReviewResponse result = reviewService.getOne(REVIEW_ID);

        assertThat(result.id()).isEqualTo(REVIEW_ID);
    }

    @Test
    @DisplayName("독후감 단건 조회 — 다른 사용자의 비공개 독후감 → REVIEW_NOT_FOUND")
    void getOne_hiddenOther_throws() {
        Review review = stubReviewForResponse(REVIEW_ID, stubUser(OTHER_ID));
        when(review.isHidden()).thenReturn(true);
        when(review.isAuthor(USER_ID)).thenReturn(false);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.getOne(REVIEW_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("재독 기록 조회 — 이전 글부터 현재 글까지 순서와 회차를 반환")
    void getRereadHistory_success() {
        User author = stubUser(USER_ID);
        Book book = mock(Book.class);
        Review first = historyReview(10L, author, book, null, LocalDateTime.of(2025, 1, 1, 10, 0), false);
        Review current = historyReview(11L, author, book, first, LocalDateTime.of(2026, 1, 1, 10, 0), false);
        when(book.getId()).thenReturn(7L);
        when(current.isAuthor(USER_ID)).thenReturn(true);
        when(reviewRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(current));
        when(reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(USER_ID, 7L))
                .thenReturn(List.of(first, current));

        ReviewRereadHistoryResponse result = reviewService.getRereadHistory(11L);

        assertThat(result.canCreateReread()).isTrue();
        assertThat(result.latestReviewId()).isEqualTo(11L);
        assertThat(result.records()).extracting(ReviewRereadHistoryResponse.HistoryItem::sequence)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("재독 기록 조회 — 다른 사용자에게는 비공개 회차를 제외")
    void getRereadHistory_filtersHiddenForOtherUser() {
        User author = stubUser(OTHER_ID);
        Book book = mock(Book.class);
        Review hiddenFirst = historyReview(10L, author, book, null, LocalDateTime.of(2025, 1, 1, 10, 0), true);
        Review current = historyReview(11L, author, book, hiddenFirst, LocalDateTime.of(2026, 1, 1, 10, 0), false);
        when(book.getId()).thenReturn(7L);
        when(reviewRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(current));
        when(reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(OTHER_ID, 7L))
                .thenReturn(List.of(hiddenFirst, current));

        ReviewRereadHistoryResponse result = reviewService.getRereadHistory(11L);

        assertThat(result.canCreateReread()).isFalse();
        assertThat(result.records()).extracting(ReviewRereadHistoryResponse.HistoryItem::id)
                .containsExactly(11L);
        assertThat(result.records().getFirst().sequence()).isEqualTo(2);
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

        reviewService.update(REVIEW_ID, new ReviewUpdateRequest(null, "수정된 내용", 3, null, null, null, null));

        verify(review).update("수정된 내용", 3, null);
    }

    @Test
    @DisplayName("독후감 수정 — 다른 사용자가 시도 → FORBIDDEN, update() 미호출")
    void update_notAuthor_throws() {
        Review review = mock(Review.class);
        when(review.isAuthor(USER_ID)).thenReturn(false);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(REVIEW_ID, new ReviewUpdateRequest(null, "수정", 3, null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        verify(review, never()).update(any(), anyInt(), any());
    }

    @Test
    @DisplayName("독후감 수정 — 재독 기록으로 연결된 글의 책 변경은 차단")
    void update_linkedReviewBookChange_throws() {
        User author = stubUser(USER_ID);
        Book originalBook = mock(Book.class);
        Book changedBook = mock(Book.class);
        Review previous = stubReviewForResponse(9L, author);
        Review review = stubReviewForResponse(REVIEW_ID, author);
        when(originalBook.getId()).thenReturn(7L);
        when(changedBook.getId()).thenReturn(8L);
        when(review.isAuthor(USER_ID)).thenReturn(true);
        when(review.getBook()).thenReturn(originalBook);
        when(review.getPreviousReview()).thenReturn(previous);
        when(reviewRepository.findByIdAndDeletedAtIsNull(REVIEW_ID)).thenReturn(Optional.of(review));
        when(bookRepository.findById(8L)).thenReturn(Optional.of(changedBook));

        assertThatThrownBy(() -> reviewService.update(
                REVIEW_ID, new ReviewUpdateRequest(8L, "수정", 3, null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONNECTED_REVIEW_BOOK_CHANGE_NOT_ALLOWED);
        verify(review, never()).update(any(), anyInt(), any());
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

    private Review historyReview(Long id, User author, Book book, Review previous,
                                 LocalDateTime createdAt, boolean hidden) {
        Review review = stubReviewForResponse(id, author);
        lenient().when(review.getBook()).thenReturn(book);
        lenient().when(review.getPreviousReview()).thenReturn(previous);
        lenient().when(review.getCreatedAt()).thenReturn(createdAt);
        lenient().when(review.isHidden()).thenReturn(hidden);
        return review;
    }
}
