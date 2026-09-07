package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileMemberRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 캠페인 운영 권한 확인.
 * 여러 서비스가 같은 규칙을 쓰므로 한 곳에 모았다.
 * 권한은 전역 역할이 아니라 official_profile_members(프로필 소속)로 판단한다.
 */
@Component
@RequiredArgsConstructor
public class CampaignAccessGuard {

    private final ReviewCampaignRepository campaignRepository;
    private final OfficialProfileMemberRepository profileMemberRepository;

    public ReviewCampaign requireCampaignAccess(Long campaignId) {
        ReviewCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND));
        requireProfileAccess(campaign.getProfile().getId());
        return campaign;
    }

    public void requireProfileAccess(Long profileId) {
        if (SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
        Long userId = SecurityUtils.getCurrentUserId();
        if (!profileMemberRepository.existsByProfileIdAndUserId(profileId, userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public ReviewCampaign requireCampaignWriteAccess(Long campaignId) {
        ReviewCampaign campaign = campaignRepository.findForUpdate(campaignId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND));
        requireProfileAccess(campaign.getProfile().getId());
        return campaign;
    }
}
