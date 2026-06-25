package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupResponse;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        verify(memberRepository, never()).save(any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any());
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
}
