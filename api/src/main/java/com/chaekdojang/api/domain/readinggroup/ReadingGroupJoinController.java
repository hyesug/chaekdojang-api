package com.chaekdojang.api.domain.readinggroup;

import com.chaekdojang.api.domain.readinggroup.dto.ReadingGroupMemberResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{slug}/joins")
@RequiredArgsConstructor
public class ReadingGroupJoinController {

    private final ReadingGroupService readingGroupService;

    @PostMapping("/{memberId}/accept")
    public ApiResponse<ReadingGroupMemberResponse> accept(
            @PathVariable String slug,
            @PathVariable Long memberId) {
        return ApiResponse.ok(readingGroupService.approveMember(slug, memberId));
    }
}
