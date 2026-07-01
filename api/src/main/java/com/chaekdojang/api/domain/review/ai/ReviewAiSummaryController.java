package com.chaekdojang.api.domain.review.ai;

import com.chaekdojang.api.domain.review.ai.dto.ReviewAiSummaryResponse;
import com.chaekdojang.api.domain.review.ai.dto.ReviewAiSummaryUpdateRequest;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/reviews/{reviewId}/ai-summary", "/api/review/{reviewId}/ai-summary"})
@RequiredArgsConstructor
public class ReviewAiSummaryController {

    private final ReviewAiSummaryService summaryService;

    @Operation(summary = "독후감 AI 요약카드 상태 조회", description = "요약 생성 상태와 완료된 요약카드 내용을 반환합니다.")
    @GetMapping("/status")
    public ApiResponse<ReviewAiSummaryResponse> getStatus(@PathVariable Long reviewId) {
        return ApiResponse.ok(summaryService.getStatus(reviewId));
    }

    @Operation(summary = "독후감 AI 요약카드 직접 수정", description = "작성자가 요약카드 4개 항목을 직접 수정합니다.")
    @PutMapping
    public ApiResponse<ReviewAiSummaryResponse> update(
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewAiSummaryUpdateRequest request) {
        return ApiResponse.ok(summaryService.update(reviewId, request));
    }

    @Operation(summary = "독후감 AI 요약카드 재생성", description = "작성자가 AI 요약카드 재생성 작업을 큐에 등록합니다.")
    @PostMapping("/regenerate")
    public ApiResponse<ReviewAiSummaryResponse> regenerate(@PathVariable Long reviewId) {
        return ApiResponse.ok(summaryService.regenerate(reviewId));
    }
}
