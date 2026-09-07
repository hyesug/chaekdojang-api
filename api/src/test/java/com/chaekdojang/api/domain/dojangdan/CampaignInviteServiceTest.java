package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import com.chaekdojang.api.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignInviteServiceTest {

    private static final Long PROFILE_ID = 10L;
    private static final Long CAMPAIGN_ID = 100L;

    @Mock ProfileFollowIntentRepository intentRepository;
    @Mock ProfileInviteSendRepository inviteSendRepository;
    @Mock NotificationService notificationService;
    @InjectMocks CampaignInviteService service;

    private OfficialProfile profile;
    private ReviewCampaign campaign;
    private User actor;

    @BeforeEach
    void setUp() {
        profile = OfficialProfile.builder()
                .type(OfficialProfileType.PUBLISHER)
                .displayName("테스트출판사")
                .slug("test-publisher")
                .build();
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);

        Book book = Book.builder()
                .title("테스트 책")
                .author("테스트 작가")
                .source(BookSource.KAKAO)
                .build();

        campaign = ReviewCampaign.builder()
                .profile(profile)
                .book(book)
                .title("두 번째 서평단")
                .recruitCount(5)
                .recruitStartAt(LocalDateTime.now())
                .recruitEndAt(LocalDateTime.now().plusDays(7))
                .reviewDueAt(LocalDateTime.now().plusDays(21))
                .priorityInviteHours(24)
                .build();
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);

        actor = user(1L, "출판사담당자");
    }

    @Test
    void 관심_독자에게_우선_초대를_보낸다() {
        when(intentRepository.findByProfileIdAndUnsubscribedAtIsNull(PROFILE_ID))
                .thenReturn(List.of(intent(user(2L, "독자A")), intent(user(3L, "독자B"))));
        when(inviteSendRepository.existsByCampaignIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(inviteSendRepository.countByProfileIdAndUserIdAndSentAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(0L);

        CampaignInviteService.InviteResult result = service.sendPriorityInvites(campaign, actor);

        assertThat(result.targetCount()).isEqualTo(2);
        assertThat(result.sentCount()).isEqualTo(2);
        verify(inviteSendRepository, times(2)).save(any(ProfileInviteSend.class));
        verify(notificationService, times(2))
                .send(any(User.class), any(User.class), any(NotificationType.class), anyLong());
    }

    @Test
    void 같은_캠페인으로_두_번_보내지_않는다() {
        User reader = user(2L, "독자A");
        when(intentRepository.findByProfileIdAndUnsubscribedAtIsNull(PROFILE_ID))
                .thenReturn(List.of(intent(reader)));
        when(inviteSendRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, 2L)).thenReturn(true);

        CampaignInviteService.InviteResult result = service.sendPriorityInvites(campaign, actor);

        assertThat(result.sentCount()).isZero();
        assertThat(result.skippedAlreadySent()).isEqualTo(1);
        verify(inviteSendRepository, never()).save(any(ProfileInviteSend.class));
        verify(notificationService, never())
                .send(any(User.class), any(User.class), any(NotificationType.class), anyLong());
    }

    @Test
    void 같은_출판사가_30일에_2회를_넘겨_보내지_않는다() {
        User reader = user(2L, "독자A");
        when(intentRepository.findByProfileIdAndUnsubscribedAtIsNull(PROFILE_ID))
                .thenReturn(List.of(intent(reader)));
        when(inviteSendRepository.existsByCampaignIdAndUserId(CAMPAIGN_ID, 2L)).thenReturn(false);
        when(inviteSendRepository.countByProfileIdAndUserIdAndSentAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(2L);

        CampaignInviteService.InviteResult result = service.sendPriorityInvites(campaign, actor);

        assertThat(result.sentCount()).isZero();
        assertThat(result.skippedRateLimited()).isEqualTo(1);
        verify(inviteSendRepository, never()).save(any(ProfileInviteSend.class));
    }

    @Test
    void 제한에_걸린_독자와_걸리지_않은_독자가_섞여도_보낼_사람에게만_보낸다() {
        when(intentRepository.findByProfileIdAndUnsubscribedAtIsNull(PROFILE_ID))
                .thenReturn(List.of(intent(user(2L, "많이받은독자")), intent(user(3L, "처음받는독자"))));
        when(inviteSendRepository.existsByCampaignIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(inviteSendRepository.countByProfileIdAndUserIdAndSentAtAfter(eq(PROFILE_ID), eq(2L), any()))
                .thenReturn(2L);
        when(inviteSendRepository.countByProfileIdAndUserIdAndSentAtAfter(eq(PROFILE_ID), eq(3L), any()))
                .thenReturn(1L);

        CampaignInviteService.InviteResult result = service.sendPriorityInvites(campaign, actor);

        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.skippedRateLimited()).isEqualTo(1);
        verify(inviteSendRepository, times(1)).save(any(ProfileInviteSend.class));
    }

    @Test
    void 관심_독자가_없으면_아무것도_보내지_않는다() {
        when(intentRepository.findByProfileIdAndUnsubscribedAtIsNull(PROFILE_ID)).thenReturn(List.of());

        CampaignInviteService.InviteResult result = service.sendPriorityInvites(campaign, actor);

        assertThat(result.targetCount()).isZero();
        assertThat(result.sentCount()).isZero();
        verify(notificationService, never())
                .send(any(User.class), any(User.class), any(NotificationType.class), anyLong());
    }

    private User user(Long id, String nickname) {
        User user = User.create(nickname + "@test.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ProfileFollowIntent intent(User user) {
        return ProfileFollowIntent.builder()
                .user(user)
                .profile(profile)
                .sourceCampaign(campaign)
                .build();
    }
}
