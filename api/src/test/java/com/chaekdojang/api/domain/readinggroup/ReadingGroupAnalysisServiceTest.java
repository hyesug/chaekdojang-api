package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingGroupAnalysisServiceTest {
    @Mock ReadingGroupRepository groupRepository;
    @Mock ReadingGroupMemberRepository memberRepository;
    @Mock ReadingGroupBookRepository groupBookRepository;
    @Mock ReadingGroupReviewRepository groupReviewRepository;
    @Mock ReadingGroupBookAiAnalysisRepository analysisRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewReflectionAiClient aiClient;
    @Mock MetricEventRepository metricEventRepository;
    @Mock MetricEventService metricEventService;

    private ReadingGroupAnalysisService service;
    private ReviewAiSummaryProperties aiProperties;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getPrincipal()).thenReturn(1L);
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        aiProperties = new ReviewAiSummaryProperties();
        aiProperties.setEnabled(true);
        aiProperties.setApiKey("test-key");
        aiProperties.setModel("test-model");
        FeedbackProperties feedbackProperties = new FeedbackProperties();
        service = new ReadingGroupAnalysisService(
                groupRepository, memberRepository, groupBookRepository, groupReviewRepository,
                analysisRepository, userRepository, aiClient, aiProperties, feedbackProperties,
                metricEventRepository, metricEventService, new ObjectMapper());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerGeneratesAndStoresWholeGroupAnalysis() {
        User owner = user(1L, "owner");
        User reader = user(2L, "reader");
        ReadingGroup group = ReadingGroup.builder()
                .owner(owner).name("함께 읽기").slug("reading-together")
                .visibility(ReadingGroupVisibility.PUBLIC).joinPolicy(ReadingGroupJoinPolicy.OPEN).build();
        ReflectionTestUtils.setField(group, "id", 10L);
        Book book = Book.builder().title("테스트 책").author("작가")
                .source(BookSource.KAKAO).build();
        ReflectionTestUtils.setField(book, "id", 20L);
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book, null);
        ReflectionTestUtils.setField(groupBook, "id", 30L);
        Review first = review(41L, book, owner, "첫 번째 독후감");
        Review second = review(42L, book, reader, "두 번째 독후감");
        ReadingGroupAnalysisResult aiResult = new ReadingGroupAnalysisResult(
                "서로 다른 시선으로 같은 주제를 살폈습니다.",
                List.of("선택의 의미를 함께 언급했습니다."),
                List.of("결말을 바라보는 관점이 달랐습니다."),
                List.of("선택", "책임"), List.of("공감", "당혹"),
                List.of("어떤 선택이 가장 기억에 남았나요?", "서로 다른 해석은 어디서 시작됐나요?"),
                100, 80);

        when(groupRepository.findBySlug("reading-together")).thenReturn(Optional.of(group));
        when(groupBookRepository.findByIdAndGroupId(30L, 10L)).thenReturn(Optional.of(groupBook));
        when(groupReviewRepository.findAllByGroupBookIdOrderByCreatedAtDesc(30L))
                .thenReturn(List.of(ReadingGroupReview.of(group, groupBook, first),
                        ReadingGroupReview.of(group, groupBook, second)));
        when(aiClient.analyzeReadingGroup(eq("테스트 책"), eq("작가"), anyList())).thenReturn(aiResult);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(analysisRepository.findByGroupBookId(30L)).thenReturn(Optional.empty());
        when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReadingGroupAnalysisResponse response = service.generate("reading-together", 30L);

        assertThat(response.commonThoughts()).containsExactly("선택의 의미를 함께 언급했습니다.");
        assertThat(response.discussionQuestions()).hasSize(2);
        assertThat(response.analyzedReviewCount()).isEqualTo(2);
        assertThat(response.stale()).isFalse();
        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_ai_analysis_succeeded"), eq(1L), anyString(), anyMap());
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@example.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Review review(Long id, Book book, User author, String content) {
        Review review = Review.builder().book(book).author(author).content(content).rating(4).build();
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "updatedAt", java.time.LocalDateTime.now());
        return review;
    }
}
