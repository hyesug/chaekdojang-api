package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupMyReviewResponse;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupResponse;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupCreateRequest;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupBookAddRequest;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupBookProgressUpdateRequest;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupNoticeUpdateRequest;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupReviewAttachRequest;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingGroupServiceTest {

    @Mock ReadingGroupRepository groupRepository;
    @Mock ReadingGroupMemberRepository memberRepository;
    @Mock ReadingGroupBookRepository groupBookRepository;
    @Mock ReadingGroupReviewRepository groupReviewRepository;
    @Mock UserRepository userRepository;
    @Mock BookRepository bookRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock NotificationService notificationService;
    @Mock MetricEventService metricEventService;

    @InjectMocks ReadingGroupService readingGroupService;

    private static final Long USER_ID = 2L;
    private static final Long OWNER_ID = 1L;
    private static final Long GROUP_ID = 10L;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication auth = mock(Authentication.class);
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

    @Test
    @DisplayName("비공개 모임 가입 신청은 이미 승인 대기 중이어도 PENDING 응답을 반환한다")
    void join_privateGroupWithPendingMember_returnsPendingResponse() {
        User owner = user(OWNER_ID, "owner");
        User applicant = user(USER_ID, "reader");
        ReadingGroup group = privateGroup(owner);
        group.updateNotice("멤버 전용 공지");
        ReadingGroupMember pendingMember = ReadingGroupMember.join(group, applicant, ReadingGroupMemberStatus.PENDING);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(applicant));
        when(groupRepository.findBySlug("private-group")).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(pendingMember));
        when(memberRepository.countByGroupIdAndStatus(GROUP_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(1L);
        when(memberRepository.existsByGroupIdAndUserIdAndStatus(GROUP_ID, USER_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(false);
        when(memberRepository.existsByGroupIdAndUserIdAndStatus(GROUP_ID, OWNER_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(true);

        ReadingGroupResponse response = readingGroupService.join("private-group");

        assertThat(response.member()).isFalse();
        assertThat(response.membershipStatus()).isEqualTo(ReadingGroupMemberStatus.PENDING);
        assertThat(response.notice()).isNull();
        verify(memberRepository, never()).save(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("비공개 모임의 내 독후감 목록에는 비공개 독후감도 포함한다")
    void getMyGroupBookReviews_privateGroup_includesHiddenReviews() {
        User owner = user(OWNER_ID, "owner");
        User member = user(USER_ID, "reader");
        ReadingGroup group = privateGroup(owner);
        Book book = book();
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book, null);
        ReflectionTestUtils.setField(groupBook, "id", 20L);
        Review hiddenReview = review(book, member, true);

        when(groupRepository.findBySlug("private-group")).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndUserIdAndStatus(
                GROUP_ID, USER_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(true);
        when(groupBookRepository.findByIdAndGroupId(20L, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(USER_ID, 30L))
                .thenReturn(List.of(hiddenReview));
        when(groupReviewRepository.existsByGroupBookIdAndReviewId(20L, 40L)).thenReturn(false);

        List<ReadingGroupMyReviewResponse> responses =
                readingGroupService.getMyGroupBookReviews("private-group", 20L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).hidden()).isTrue();
        verify(reviewRepository, never())
                .findAllByAuthorIdAndBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(anyLong(), anyLong());
    }

    @Test
    @DisplayName("공개 모임의 내 독후감 목록은 공개 독후감만 조회한다")
    void getMyGroupBookReviews_publicGroup_usesPublicReviewsOnly() {
        User owner = user(OWNER_ID, "owner");
        User member = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        Book book = book();
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book, null);
        ReflectionTestUtils.setField(groupBook, "id", 20L);
        Review publicReview = review(book, member, false);

        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndUserIdAndStatus(
                GROUP_ID, USER_ID, ReadingGroupMemberStatus.APPROVED)).thenReturn(true);
        when(groupBookRepository.findByIdAndGroupId(20L, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(USER_ID, 30L))
                .thenReturn(List.of(publicReview));
        when(groupReviewRepository.existsByGroupBookIdAndReviewId(20L, 40L)).thenReturn(false);

        List<ReadingGroupMyReviewResponse> responses =
                readingGroupService.getMyGroupBookReviews("public-group", 20L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).hidden()).isFalse();
        verify(reviewRepository, never())
                .findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(anyLong(), anyLong());
    }

    @Test
    @DisplayName("독서모임 생성 이벤트에 사용자와 모임 ID가 기록된다")
    void create_recordsActivityEvent() {
        User owner = user(USER_ID, "reader");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(groupRepository.save(any())).thenAnswer(invocation -> {
            ReadingGroup group = invocation.getArgument(0);
            ReflectionTestUtils.setField(group, "id", GROUP_ID);
            return group;
        });
        when(groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of());

        readingGroupService.create(new ReadingGroupCreateRequest(
                "Activity Group", null, null, ReadingGroupVisibility.PUBLIC, ReadingGroupJoinPolicy.OPEN));

        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_created"), eq(USER_ID), eq("/groups/activity-group"),
                argThat(meta -> GROUP_ID.equals(meta.get("groupId"))));
    }

    @Test
    @DisplayName("공개 독서모임 가입 이벤트가 기록된다")
    void join_recordsActivityEvent() {
        User owner = user(OWNER_ID, "owner");
        User applicant = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(applicant));
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.empty());
        when(groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of());

        readingGroupService.join("public-group");

        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_joined"), eq(USER_ID), eq("/groups/public-group"),
                argThat(meta -> GROUP_ID.equals(meta.get("groupId"))));
    }

    @Test
    @DisplayName("독서모임 책 추가 이벤트에 책 ID만 기록되고 본문은 기록되지 않는다")
    void addBook_recordsActivityEvent() {
        User owner = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        Book book = book();
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));
        when(groupBookRepository.existsByGroupIdAndBookId(GROUP_ID, 30L)).thenReturn(false);
        when(groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of());

        readingGroupService.addBook("public-group", new ReadingGroupBookAddRequest(30L, "운영 메모"));

        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_book_added"), eq(USER_ID), eq("/groups/public-group"),
                argThat(meta -> Long.valueOf(30L).equals(meta.get("bookId")) && !meta.containsKey("note")));
    }

    @Test
    @DisplayName("모임장은 공지를 수정할 수 있고 공지 내용은 활동 로그에 저장하지 않는다")
    void updateNotice_owner_updatesWithoutLoggingContent() {
        User owner = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of());

        ReadingGroupResponse response = readingGroupService.updateNotice(
                "public-group", new ReadingGroupNoticeUpdateRequest("금요일 오후 8시에 만나요."));

        assertThat(response.notice()).isEqualTo("금요일 오후 8시에 만나요.");
        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_notice_updated"), eq(USER_ID), eq("/groups/public-group"),
                argThat(meta -> !meta.containsKey("notice")));
    }

    @Test
    @DisplayName("모임장은 선정 책의 진행 상태와 마감일을 수정할 수 있다")
    void updateBookProgress_owner_updatesStatusAndDeadline() {
        User owner = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book(), null);
        ReflectionTestUtils.setField(groupBook, "id", 20L);
        LocalDate deadline = LocalDate.of(2026, 8, 31);
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(groupBookRepository.findByIdAndGroupId(20L, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(groupBookRepository.findAllByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of(groupBook));

        ReadingGroupResponse response = readingGroupService.updateBookProgress(
                "public-group", 20L,
                new ReadingGroupBookProgressUpdateRequest(ReadingGroupBookStatus.READING, deadline));

        assertThat(response.books().get(0).status()).isEqualTo(ReadingGroupBookStatus.READING);
        assertThat(response.books().get(0).deadline()).isEqualTo(deadline);
        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_book_progress_updated"), eq(USER_ID),
                eq("/groups/public-group/books/20"),
                argThat(meta -> "READING".equals(meta.get("status")) && !meta.containsKey("content")));
    }

    @Test
    @DisplayName("일반 회원은 선정 책 진행 상태를 수정할 수 없다")
    void updateBookProgress_nonManager_isForbidden() {
        User owner = user(OWNER_ID, "owner");
        ReadingGroup group = publicGroup(owner);
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> readingGroupService.updateBookProgress(
                "public-group", 20L,
                new ReadingGroupBookProgressUpdateRequest(ReadingGroupBookStatus.READING, null)))
                .isInstanceOf(CustomException.class);

        verify(groupBookRepository, never()).findByIdAndGroupId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("독서모임 독후감 연결 이벤트에는 대상 ID만 기록되고 독후감 본문은 기록되지 않는다")
    void attachReview_recordsIdsWithoutContent() {
        User owner = user(USER_ID, "reader");
        ReadingGroup group = publicGroup(owner);
        Book book = book();
        ReadingGroupBook groupBook = ReadingGroupBook.of(group, book, null);
        ReflectionTestUtils.setField(groupBook, "id", 20L);
        Review review = review(book, owner, false);
        when(groupRepository.findBySlug("public-group")).thenReturn(Optional.of(group));
        when(groupBookRepository.findByIdAndGroupId(20L, GROUP_ID)).thenReturn(Optional.of(groupBook));
        when(reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(40L)).thenReturn(Optional.of(review));
        when(groupReviewRepository.existsByGroupBookIdAndReviewId(20L, 40L)).thenReturn(false);
        when(groupReviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        readingGroupService.attachReview("public-group", 20L, new ReadingGroupReviewAttachRequest(40L));

        verify(metricEventService).recordCurrentRequestEvent(
                eq("reading_group_review_attached"), eq(USER_ID), eq("/groups/public-group/books/20"),
                argThat(meta -> Long.valueOf(40L).equals(meta.get("reviewId")) && !meta.containsKey("content")));
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@example.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ReadingGroup privateGroup(User owner) {
        ReadingGroup group = ReadingGroup.builder()
                .owner(owner)
                .name("Private Group")
                .slug("private-group")
                .visibility(ReadingGroupVisibility.PRIVATE)
                .joinPolicy(ReadingGroupJoinPolicy.APPROVAL)
                .build();
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        return group;
    }

    private ReadingGroup publicGroup(User owner) {
        ReadingGroup group = ReadingGroup.builder()
                .owner(owner)
                .name("Public Group")
                .slug("public-group")
                .visibility(ReadingGroupVisibility.PUBLIC)
                .joinPolicy(ReadingGroupJoinPolicy.OPEN)
                .build();
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        return group;
    }

    private Book book() {
        Book book = Book.builder()
                .isbn13("9781234567890")
                .title("테스트 책")
                .author("테스트 작가")
                .source(BookSource.KAKAO)
                .build();
        ReflectionTestUtils.setField(book, "id", 30L);
        return book;
    }

    private Review review(Book book, User author, boolean hidden) {
        Review review = Review.builder()
                .book(book)
                .author(author)
                .content("테스트 독후감")
                .rating(5)
                .build();
        ReflectionTestUtils.setField(review, "id", 40L);
        if (hidden) review.hide();
        return review;
    }
}
