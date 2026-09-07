package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.MyFollowIntentResponse;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "다음 책 소식 받기" 의사 관리.
 * 독자는 언제든 1클릭으로 해제할 수 있어야 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileFollowIntentService {

    private final ProfileFollowIntentRepository intentRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;

    /** 신청 시점에 기록한다. 이미 있으면 되살린다. */
    @Transactional
    public void record(User user, OfficialProfile profile, ReviewCampaign sourceCampaign) {
        intentRepository.findByUserIdAndProfileId(user.getId(), profile.getId())
                .ifPresentOrElse(
                        intent -> intent.resubscribe(sourceCampaign),
                        () -> intentRepository.save(ProfileFollowIntent.builder()
                                .user(user)
                                .profile(profile)
                                .sourceCampaign(sourceCampaign)
                                .build())
                );
    }

    /** 미선정 통보 안에서 재확인할 때 쓴다. */
    @Transactional
    public void updateFromApplication(Long applicationId, boolean subscribe) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReviewCampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (subscribe) {
            record(application.getUser(), application.getCampaign().getProfile(), application.getCampaign());
        } else {
            intentRepository
                    .findByUserIdAndProfileId(userId, application.getCampaign().getProfile().getId())
                    .ifPresent(ProfileFollowIntent::unsubscribe);
        }
    }

    /** 수신 거부 1클릭 해제 */
    @Transactional
    public void unsubscribe(Long profileId) {
        intentRepository.findByUserIdAndProfileId(SecurityUtils.getCurrentUserId(), profileId)
                .ifPresent(ProfileFollowIntent::unsubscribe);
    }

    public List<MyFollowIntentResponse> getMyIntents() {
        return intentRepository
                .findByUserIdAndUnsubscribedAtIsNullOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .stream()
                .map(MyFollowIntentResponse::from)
                .toList();
    }

    public boolean isSubscribed(Long userId, Long profileId) {
        return intentRepository.existsByUserIdAndProfileIdAndUnsubscribedAtIsNull(userId, profileId);
    }
}
