package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.officialprofile.OfficialProfileMemberRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공모전 운영 권한 확인.
 * 권한은 전역 역할이 아니라 official_profile_members(프로필 소속)로 판단한다.
 * 관리자는 책도장이 직접 주최하는 공모전을 운영해야 하므로 모든 프로필에 접근할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ContestAccessGuard {

    private final ContestRepository contestRepository;
    private final OfficialProfileMemberRepository profileMemberRepository;

    public Contest requireContestAccess(Long contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEST_NOT_FOUND));
        requireProfileAccess(contest.getHost().getId());
        return contest;
    }

    public Contest requireContestWriteAccess(Long contestId) {
        Contest contest = contestRepository.findForUpdate(contestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEST_NOT_FOUND));
        requireProfileAccess(contest.getHost().getId());
        return contest;
    }

    public void requireProfileAccess(Long profileId) {
        if (SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) return;
        Long userId = SecurityUtils.getCurrentUserId();
        if (!profileMemberRepository.existsByProfileIdAndUserId(profileId, userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
