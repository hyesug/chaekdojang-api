package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupMemberResponse;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{slug}/joins")
@RequiredArgsConstructor
public class ReadingGroupJoinController {

    private final ReadingGroupRepository groupRepository;
    private final ReadingGroupMemberRepository memberRepository;

    @PostMapping("/{memberId}/accept")
    @Transactional
    public ApiResponse<ReadingGroupMemberResponse> accept(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReadingGroup group = groupRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        ReadingGroupMember actor = memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        if (!actor.canManage()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        ReadingGroupMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!target.getGroup().getId().equals(group.getId())) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        target.approve();
        return ApiResponse.ok(ReadingGroupMemberResponse.from(target));
    }
}
