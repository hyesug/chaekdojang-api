package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.contest.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contests/manage")
@RequiredArgsConstructor
public class ContestManageController {

    private final ContestManageService manageService;

    @GetMapping("/profiles")
    public ApiResponse<List<HostProfileResponse>> getHostProfiles() {
        return ApiResponse.ok(manageService.getHostProfiles());
    }

    @PostMapping("/profiles/{profileId}/contests")
    public ApiResponse<ManageContestDetailResponse> createContest(
            @PathVariable Long profileId,
            @RequestBody @Valid ContestCreateRequest request) {
        return ApiResponse.ok(manageService.createContest(profileId, request));
    }

    @GetMapping("/contests")
    public ApiResponse<List<ContestSummaryResponse>> getMyContests() {
        return ApiResponse.ok(manageService.getMyContests());
    }

    @GetMapping("/contests/{contestId}")
    public ApiResponse<ManageContestDetailResponse> getContest(@PathVariable Long contestId) {
        return ApiResponse.ok(manageService.getContestDetail(contestId));
    }

    @PatchMapping("/contests/{contestId}")
    public ApiResponse<ManageContestDetailResponse> updateContest(
            @PathVariable Long contestId,
            @RequestBody @Valid ContestUpdateRequest request) {
        return ApiResponse.ok(manageService.updateContest(contestId, request));
    }

    @PatchMapping("/contests/{contestId}/status")
    public ApiResponse<ManageContestDetailResponse> updateStatus(
            @PathVariable Long contestId,
            @RequestBody @Valid ContestStatusUpdateRequest request) {
        return ApiResponse.ok(manageService.updateStatus(contestId, request));
    }

    @GetMapping("/contests/{contestId}/entries")
    public ApiResponse<List<ContestEntryResponse>> getEntries(@PathVariable Long contestId) {
        return ApiResponse.ok(manageService.getEntries(contestId));
    }

    @PostMapping("/contests/{contestId}/awards")
    public ApiResponse<List<ContestEntryResponse>> saveAwards(
            @PathVariable Long contestId,
            @RequestBody @Valid ContestAwardSaveRequest request) {
        return ApiResponse.ok(manageService.saveAwards(contestId, request));
    }
}
