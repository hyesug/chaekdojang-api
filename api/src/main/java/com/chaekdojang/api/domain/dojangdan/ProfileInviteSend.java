package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 우선 초대 발송 이력.
 * 스팸이 되면 이 기능 전체가 죽으므로, 발송 제한 판단의 유일한 근거로 삼는다.
 */
@Entity
@Table(name = "profile_invite_sends",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ProfileInviteSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private OfficialProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private ReviewCampaign campaign;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Builder
    private ProfileInviteSend(OfficialProfile profile, User user, ReviewCampaign campaign) {
        this.profile = profile;
        this.user = user;
        this.campaign = campaign;
    }
}
