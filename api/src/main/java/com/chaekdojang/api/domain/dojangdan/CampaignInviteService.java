package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관심 독자 우선 초대 발송.
 *
 * 발송 제한은 느슨하게 두지 않는다. 스팸이 되는 순간 이 기능 전체가 죽기 때문이다.
 * - 같은 캠페인으로 같은 독자에게 두 번 보내지 않는다.
 * - 같은 출판사가 같은 독자에게 30일 안에 2회를 넘겨 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CampaignInviteService {

    static final int MAX_SENDS_PER_PROFILE_IN_WINDOW = 2;
    static final int SEND_WINDOW_DAYS = 30;

    private final ProfileFollowIntentRepository intentRepository;
    private final ProfileInviteSendRepository inviteSendRepository;
    private final NotificationService notificationService;

    @Transactional
    public InviteResult sendPriorityInvites(ReviewCampaign campaign, User actor) {
        Long profileId = campaign.getProfile().getId();
        List<ProfileFollowIntent> intents =
                intentRepository.findByProfileIdAndUnsubscribedAtIsNull(profileId);
        LocalDateTime since = LocalDateTime.now().minusDays(SEND_WINDOW_DAYS);

        int sent = 0;
        int skippedAlreadySent = 0;
        int skippedRateLimited = 0;

        for (ProfileFollowIntent intent : intents) {
            Long userId = intent.getUser().getId();

            if (inviteSendRepository.existsByCampaignIdAndUserId(campaign.getId(), userId)) {
                skippedAlreadySent++;
                continue;
            }
            if (inviteSendRepository.countByProfileIdAndUserIdAndSentAtAfter(profileId, userId, since)
                    >= MAX_SENDS_PER_PROFILE_IN_WINDOW) {
                skippedRateLimited++;
                continue;
            }

            inviteSendRepository.save(ProfileInviteSend.builder()
                    .profile(campaign.getProfile())
                    .user(intent.getUser())
                    .campaign(campaign)
                    .build());
            notificationService.send(intent.getUser(), actor,
                    NotificationType.CAMPAIGN_INVITED, campaign.getId());
            sent++;
        }

        return new InviteResult(intents.size(), sent, skippedAlreadySent, skippedRateLimited);
    }

    public record InviteResult(
            int targetCount,
            int sentCount,
            int skippedAlreadySent,
            int skippedRateLimited
    ) {
    }
}
