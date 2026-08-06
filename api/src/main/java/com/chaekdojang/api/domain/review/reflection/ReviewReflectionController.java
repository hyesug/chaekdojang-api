package com.chaekdojang.api.domain.review.reflection;

import com.chaekdojang.api.domain.review.reflection.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/{reviewId}/reflection")
@RequiredArgsConstructor
public class ReviewReflectionController {
    private final ReviewReflectionService reflectionService;

    @GetMapping("/follow-up")
    public ApiResponse<FollowUpQuestionResponse> getFollowUp(@PathVariable Long reviewId) {
        return ApiResponse.ok(reflectionService.getFollowUp(reviewId));
    }

    @Operation(summary = "AI 후속 질문 생성", description = "작성자가 요청할 때만 생성하며 기본적으로 같은 본문에는 저장 결과를 재사용합니다.")
    @PostMapping("/follow-up")
    public ApiResponse<FollowUpQuestionResponse> generateFollowUp(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "false") boolean regenerate) {
        return ApiResponse.ok(reflectionService.generateFollowUp(reviewId, regenerate));
    }

    @PutMapping("/follow-up")
    public ApiResponse<FollowUpQuestionResponse> updateFollowUp(
            @PathVariable Long reviewId, @RequestBody @Valid FollowUpQuestionUpdateRequest request) {
        return ApiResponse.ok(reflectionService.updateFollowUp(reviewId, request));
    }

    @DeleteMapping("/follow-up")
    public ApiResponse<Void> deleteFollowUp(@PathVariable Long reviewId) {
        reflectionService.deleteFollowUp(reviewId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/change-comparison")
    public ApiResponse<ChangeComparisonResponse> getComparison(@PathVariable Long reviewId) {
        return ApiResponse.ok(reflectionService.getComparison(reviewId));
    }

    @Operation(summary = "재독 생각 변화 AI 비교", description = "작성자가 요청할 때만 이전 재독 글과 현재 글을 비교합니다.")
    @PostMapping("/change-comparison")
    public ApiResponse<ChangeComparisonResponse> generateComparison(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "false") boolean regenerate) {
        return ApiResponse.ok(reflectionService.generateComparison(reviewId, regenerate));
    }

    @PutMapping("/change-comparison")
    public ApiResponse<ChangeComparisonResponse> updateComparison(
            @PathVariable Long reviewId, @RequestBody @Valid ChangeComparisonUpdateRequest request) {
        return ApiResponse.ok(reflectionService.updateComparison(reviewId, request));
    }

    @DeleteMapping("/change-comparison")
    public ApiResponse<Void> deleteComparison(@PathVariable Long reviewId) {
        reflectionService.deleteComparison(reviewId);
        return ApiResponse.ok(null);
    }
}
