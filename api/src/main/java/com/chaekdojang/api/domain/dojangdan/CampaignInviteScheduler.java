package com.chaekdojang.api.domain.dojangdan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignInviteScheduler {
    private final ReviewCampaignRepository campaignRepository;
    private final CampaignInviteService inviteService;

    @Scheduled(fixedDelayString = "${app.dojangdan.invite-scheduler-delay-ms:30000}")
    public void sendPendingInvites() {
        for (Long id : campaignRepository.findPendingInviteCampaignIds(LocalDateTime.now())) {
            try {
                // 캠페인별 트랜잭션. 실패한 캠페인만 다음 주기에 다시 시도한다.
                inviteService.sendDueInvites(id);
            } catch (RuntimeException e) {
                log.error("우선 초대 발송 실패. campaignId={}", id, e);
            }
        }
    }
}
