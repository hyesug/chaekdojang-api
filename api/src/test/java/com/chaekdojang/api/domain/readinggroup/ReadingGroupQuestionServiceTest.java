package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.feedback.FeedbackProperties;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupQuestionAnswerRequest;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryProperties;
import com.chaekdojang.api.domain.review.reflection.ReadingGroupQuestionResult;
import com.chaekdojang.api.domain.review.reflection.ReviewReflectionAiClient;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingGroupQuestionServiceTest {
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long GROUP_ID = 10L;
    private static final Long GROUP_BOOK_ID = 20L;

    @Mock ReadingGroupRepository groupRepository;
    @Mock ReadingGroupMemberRepository memberRepository;
    @Mock ReadingGroupBookRepository groupBookRepository;
    @Mock ReadingGroupQuestionRepository questionRepository;
    @Mock ReadingGroupQuestionResponseRepository responseRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewReflectionAiClient aiClient;
    @Mock ReviewAiSummaryProperties aiProperties;
    @Mock FeedbackProperties feedbackProperties;
    @Mock MetricEventRepository metricEventRepository;
    @Mock MetricEventService metricEventService;
    @InjectMocks ReadingGroupQuestionService service;

    @BeforeEach
    void setUp() {
        authenticate(OWNER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aiQuestionIsSavedAsUnpublishedDraftWithUsageLog() {
        User owner = user(OWNER_ID, "owner");
        ReadingGroup group = group(owner);
        ReadingGroupBook groupBook = groupBook(group);
        when(groupRepository.findBySlug("book-club")).thenReturn(Optional.of(group));
        when(groupBookRepository.findByIdAndGroupId(GROUP_BOOK_ID, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(questionRepository.findAllByGroupBookIdOrderByCreatedAtAsc(GROUP_BOOK_ID)).thenReturn(List.of());
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getApiKey()).thenReturn("configured");
        when(aiProperties.getModel()).thenReturn("test-model");
        when(feedbackProperties.getMemberDailyLimit()).thenReturn(5);
        when(aiClient.createReadingGroupQuestion(anyString(), anyString(), anyList()))
                .thenReturn(new ReadingGroupQuestionResult("어떤 생각에 가장 오래 머물렀나요?", 120, 18));
        when(questionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReadingGroupQuestion value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 30L);
            return value;
        });

        var result = service.generateAiDraft("book-club", GROUP_BOOK_ID);

        assertThat(result.aiSuggested()).isTrue();
        assertThat(result.published()).isFalse();
        ArgumentCaptor<ReadingGroupQuestion> captor = ArgumentCaptor.forClass(ReadingGroupQuestion.class);
        verify(questionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getModel()).isEqualTo("test-model");
        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_ai_question_succeeded"), eq(OWNER_ID),
                eq("/groups/book-club/books/20"),
                argThat(meta -> Long.valueOf(120).equals(meta.get("inputTokens"))
                        && Long.valueOf(18).equals(meta.get("outputTokens"))));
    }

    @Test
    void completedGroupBookRejectsNewIntermediateAnswer() {
        authenticate(MEMBER_ID);
        User owner = user(OWNER_ID, "owner");
        ReadingGroup group = group(owner);
        ReadingGroupBook groupBook = groupBook(group);
        groupBook.updateProgress(ReadingGroupBookStatus.COMPLETED, null, null);
        ReadingGroupQuestion question = ReadingGroupQuestion.manual(groupBook, owner, "질문");
        ReflectionTestUtils.setField(question, "id", 30L);
        when(groupRepository.findBySlug("book-club")).thenReturn(Optional.of(group));
        when(groupBookRepository.findByIdAndGroupId(GROUP_BOOK_ID, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(memberRepository.existsByGroupIdAndUserIdAndStatus(
                GROUP_ID, MEMBER_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(true);

        assertThatThrownBy(() -> service.saveAnswer(
                "book-club", GROUP_BOOK_ID, 30L, new ReadingGroupQuestionAnswerRequest("내 생각")))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.GROUP_BOOK_ARCHIVED));
        verify(responseRepository, never()).saveAndFlush(any());
    }

    private void authenticate(Long userId) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(userId);
        lenient().when(authentication.getAuthorities()).thenReturn(List.of());
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@example.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ReadingGroup group(User owner) {
        ReadingGroup group = ReadingGroup.builder()
                .owner(owner).name("Book Club").slug("book-club")
                .visibility(ReadingGroupVisibility.PUBLIC).joinPolicy(ReadingGroupJoinPolicy.OPEN)
                .build();
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        return group;
    }

    private ReadingGroupBook groupBook(ReadingGroup group) {
        Book book = Book.builder().title("테스트 책").author("작가").source(BookSource.KAKAO).build();
        ReflectionTestUtils.setField(book, "id", 40L);
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book, null);
        ReflectionTestUtils.setField(groupBook, "id", GROUP_BOOK_ID);
        return groupBook;
    }
}
