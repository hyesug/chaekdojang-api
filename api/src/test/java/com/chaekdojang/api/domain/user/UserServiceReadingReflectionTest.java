package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.chat.ChatBlockRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.notification.NotificationRepository;
import com.chaekdojang.api.domain.readinggoal.ReadingGoalRepository;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewBookmarkRepository;
import com.chaekdojang.api.domain.review.ReviewLikeRepository;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.subscription.SubscriptionRepository;
import com.chaekdojang.api.domain.user.dto.ReadingReflectionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceReadingReflectionTest {

    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock ChatBlockRepository chatBlockRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;
    @Mock ReviewBookmarkRepository reviewBookmarkRepository;
    @Mock LibraryRepository libraryRepository;
    @Mock BookRepository bookRepository;
    @Mock ReadingGoalRepository readingGoalRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserAuthProviderRepository userAuthProviderRepository;
    @Mock MetricEventRepository metricEventRepository;
    @Mock InquiryRepository inquiryRepository;
    @Mock MetricEventService metricEventService;
    @InjectMocks UserService userService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onlyExplicitPreviousReviewLinksCountAsRereads() {
        User author = User.create("reader@example.com", "reader", null);
        ReflectionTestUtils.setField(author, "id", 7L);
        Book stranger = book(227L, "이방인");
        Book linkedBook = book(409L, "진짜 재독한 책");

        Review strangerFirst = review(1L, author, stranger, null, LocalDateTime.of(2026, 1, 1, 10, 0));
        Review strangerSecond = review(2L, author, stranger, null, LocalDateTime.of(2026, 2, 1, 10, 0));
        Review rereadFirst = review(3L, author, linkedBook, null, LocalDateTime.of(2026, 3, 1, 10, 0));
        Review rereadSecond = review(4L, author, linkedBook, rereadFirst, LocalDateTime.of(2026, 4, 1, 10, 0));
        when(reviewRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(strangerFirst, strangerSecond, rereadFirst, rereadSecond));

        ReadingReflectionResponse result = userService.getReadingReflection();

        assertThat(result.rereadCount()).isEqualTo(1);
        assertThat(result.rereadBooks()).singleElement().satisfies(book -> {
            assertThat(book.bookId()).isEqualTo(409L);
            assertThat(book.recordCount()).isEqualTo(2);
        });
        assertThat(result.longestRecordedBook()).isNotNull();
        assertThat(result.longestRecordedBook().bookId()).isEqualTo(409L);
    }

    @Test
    void infersGenreTimelineWhenKakaoBooksHaveNoCategory() {
        User author = User.create("reader@example.com", "reader", null);
        ReflectionTestUtils.setField(author, "id", 7L);
        Book novel = Book.builder()
                .title("급류")
                .author("정대건")
                .description("두 인물의 사랑을 그린 두 번째 장편소설")
                .source(BookSource.KAKAO)
                .build();
        ReflectionTestUtils.setField(novel, "id", 110L);
        when(reviewRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(review(1L, author, novel, null,
                        LocalDateTime.of(2026, 7, 1, 10, 0))));

        ReadingReflectionResponse result = userService.getReadingReflection();

        assertThat(result.genreTimeline()).singleElement().satisfies(genre -> {
            assertThat(genre.year()).isEqualTo(2026);
            assertThat(genre.genre()).isEqualTo("소설");
            assertThat(genre.count()).isEqualTo(1);
        });
    }

    private Book book(Long id, String title) {
        Book book = Book.builder().title(title).author("작가").source(BookSource.KAKAO).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Review review(Long id, User author, Book book, Review previous, LocalDateTime createdAt) {
        Review review = Review.builder()
                .author(author)
                .book(book)
                .previousReview(previous)
                .content("독후감")
                .rating(3)
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", createdAt);
        return review;
    }
}
