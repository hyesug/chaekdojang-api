package com.chaekdojang.api.domain.dojangdan;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewCampaignPriorityTest {
    @Test
    void 예약_모집은_시작일부터_우선_신청_기간을_갖는다() {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        ReviewCampaign campaign = ReviewCampaign.builder().title("예약 모집").recruitCount(5)
                .recruitStartAt(start).recruitEndAt(start.plusDays(7)).reviewDueAt(start.plusDays(14))
                .priorityInviteHours(24).build();
        campaign.startRecruiting();
        assertThat(campaign.isAcceptingApplications(start.minusSeconds(1))).isFalse();
        assertThat(campaign.isInPriorityWindow(start.minusSeconds(1))).isFalse();
        assertThat(campaign.getPriorityInviteUntil()).isEqualTo(start.plusHours(24));
        assertThat(campaign.isAcceptingApplications(start)).isTrue();
        assertThat(campaign.isInPriorityWindow(start)).isTrue();
        assertThat(campaign.isInPriorityWindow(start.plusHours(24))).isFalse();
    }

    @Test
    void 발송_완료_후_다시_열어도_우선_기간을_연장하지_않는다() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        ReviewCampaign campaign = ReviewCampaign.builder().title("모집").recruitCount(5)
                .recruitStartAt(start).recruitEndAt(start.plusDays(7)).reviewDueAt(start.plusDays(14))
                .priorityInviteHours(48).build();
        campaign.startRecruiting();
        LocalDateTime until = campaign.getPriorityInviteUntil();
        campaign.markPriorityInvitesSent(LocalDateTime.now());
        campaign.changeStatus(CampaignStatus.CLOSED);
        campaign.startRecruiting();
        assertThat(campaign.getPriorityInviteUntil()).isEqualTo(until);
    }

    @Test
    void 모집을_늦게_열어도_우선_기간은_입력한_시작일을_기준으로_한다() {
        LocalDateTime start = LocalDateTime.now().minusHours(10);
        ReviewCampaign campaign = ReviewCampaign.builder().title("늦게 연 모집").recruitCount(5)
                .recruitStartAt(start).recruitEndAt(start.plusDays(7)).reviewDueAt(start.plusDays(14))
                .priorityInviteHours(24).build();
        campaign.startRecruiting();
        assertThat(campaign.getPriorityInviteUntil()).isEqualTo(start.plusHours(24));
    }
}
