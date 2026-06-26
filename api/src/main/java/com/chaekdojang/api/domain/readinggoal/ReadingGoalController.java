package com.chaekdojang.api.domain.readinggoal;

import com.chaekdojang.api.domain.readinggoal.dto.ReadingGoalResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "독서 목표", description = "올해 독서 목표 권수 설정과 진행률 조회")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReadingGoalController {

    private final ReadingGoalService readingGoalService;

    @Operation(summary = "내 독서 목표 조회", description = "올해 독서 목표와 완독 진행률을 반환합니다. JWT 필요.")
    @GetMapping("/me/reading-goal")
    public ApiResponse<ReadingGoalResponse> getMyGoal() {
        return ApiResponse.ok(readingGoalService.getMyGoal());
    }

    @Operation(summary = "공개 독서 목표 조회", description = "공개 설정된 올해 독서 목표와 완독 진행률을 반환합니다. 인증 불필요.")
    @GetMapping("/{userId}/reading-goal")
    public ApiResponse<ReadingGoalResponse> getPublicGoal(@PathVariable Long userId) {
        return ApiResponse.ok(readingGoalService.getPublicGoal(userId));
    }
}
