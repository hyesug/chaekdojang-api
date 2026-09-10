package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.contest.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @GetMapping
    public ApiResponse<List<ContestSummaryResponse>> getContests() {
        return ApiResponse.ok(contestService.getOpenContests());
    }

    @GetMapping("/{contestId}")
    public ApiResponse<ContestDetailResponse> getContest(@PathVariable Long contestId) {
        return ApiResponse.ok(contestService.getContest(contestId));
    }

    @GetMapping("/{contestId}/submittable-reviews")
    public ApiResponse<List<ContestSubmittableReviewResponse>> getSubmittableReviews(
            @PathVariable Long contestId) {
        return ApiResponse.ok(contestService.getSubmittableReviews(contestId));
    }

    @PostMapping("/{contestId}/entries")
    public ApiResponse<MyContestEntryResponse> submit(
            @PathVariable Long contestId,
            @RequestBody @Valid ContestEntrySubmitRequest request) {
        return ApiResponse.ok(contestService.submit(contestId, request));
    }

    @GetMapping("/me/entries")
    public ApiResponse<List<MyContestEntryResponse>> getMyEntries() {
        return ApiResponse.ok(contestService.getMyEntries());
    }

    @PostMapping("/me/entries/{entryId}/withdraw")
    public ApiResponse<MyContestEntryResponse> withdraw(@PathVariable Long entryId) {
        return ApiResponse.ok(contestService.withdraw(entryId));
    }
}
