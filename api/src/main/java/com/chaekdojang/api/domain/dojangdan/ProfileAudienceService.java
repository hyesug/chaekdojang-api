package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.ProfileAudienceResponse;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileRepository;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 출판사 화면에 보여줄 관심 독자 집계.
 * 누가 관심을 표시했는지는 절대 내려주지 않는다. 숫자와 분포만 넘긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileAudienceService {

    private static final int TOP_CATEGORY_LIMIT = 5;

    private final ProfileFollowIntentRepository intentRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final OfficialProfileRepository profileRepository;
    private final CampaignAccessGuard accessGuard;

    public ProfileAudienceResponse getAudience(Long profileId) {
        accessGuard.requireProfileAccess(profileId);
        OfficialProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        List<Long> readerIds = intentRepository.findByProfileIdAndUnsubscribedAtIsNull(profileId)
                .stream()
                .map(intent -> intent.getUser().getId())
                .toList();

        if (readerIds.isEmpty()) {
            return new ProfileAudienceResponse(profileId, profile.getDisplayName(), 0, 0, List.of());
        }

        long experienced = applicationRepository.findByUserIdIn(readerIds).stream()
                .filter(application -> application.getSelectedAt() != null)
                .map(application -> application.getUser().getId())
                .distinct()
                .count();

        return new ProfileAudienceResponse(
                profileId,
                profile.getDisplayName(),
                readerIds.size(),
                experienced,
                topCategories(readerIds)
        );
    }

    /** 관심 독자들이 쓴 독후감의 책 분류로 관심 장르를 추정한다. */
    private List<ProfileAudienceResponse.CategoryCount> topCategories(List<Long> readerIds) {
        Map<String, Long> counts = reviewRepository
                .findAllByAuthorIdInAndDeletedAtIsNullOrderByCreatedAtDesc(readerIds)
                .stream()
                .map(review -> review.getBook() == null ? null : review.getBook().getCategory())
                .filter(category -> category != null && !category.isBlank())
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_CATEGORY_LIMIT)
                .map(entry -> new ProfileAudienceResponse.CategoryCount(entry.getKey(), entry.getValue()))
                .toList();
    }
}
