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
 * "이 출판사·작가의 다음 책 소식을 받겠다"는 독자의 의사.
 * 서평단에 신청했다 선정되지 않은 독자를 다음 캠페인으로 잇는 연결고리다.
 */
@Entity
@Table(name = "profile_follow_intents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "profile_id"}))
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ProfileFollowIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private OfficialProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_campaign_id")
    private ReviewCampaign sourceCampaign;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime unsubscribedAt;

    @Builder
    private ProfileFollowIntent(User user, OfficialProfile profile, ReviewCampaign sourceCampaign) {
        this.user = user;
        this.profile = profile;
        this.sourceCampaign = sourceCampaign;
    }

    public void unsubscribe() {
        if (this.unsubscribedAt == null) {
            this.unsubscribedAt = LocalDateTime.now();
        }
    }

    /** 다시 받겠다고 하면 기존 행을 되살린다. 중복 행을 만들지 않기 위해서다. */
    public void resubscribe(ReviewCampaign sourceCampaign) {
        this.unsubscribedAt = null;
        if (sourceCampaign != null) {
            this.sourceCampaign = sourceCampaign;
        }
    }

    public boolean isActive() {
        return unsubscribedAt == null;
    }
}
