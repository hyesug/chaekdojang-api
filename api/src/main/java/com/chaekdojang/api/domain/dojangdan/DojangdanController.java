package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dojangdan")
@RequiredArgsConstructor
public class DojangdanController {

    private final DojangdanService dojangdanService;

    @GetMapping("/campaigns")
    public ApiResponse<List<CampaignSummaryResponse>> getCampaigns() {
        return ApiResponse.ok(dojangdanService.getOpenCampaigns());
    }

    @GetMapping("/campaigns/{campaignId}")
    public ApiResponse<CampaignDetailResponse> getCampaign(@PathVariable Long campaignId) {
        return ApiResponse.ok(dojangdanService.getCampaign(campaignId));
    }

    @PostMapping("/campaigns/{campaignId}/applications")
    public ApiResponse<MyCampaignApplicationResponse> apply(
            @PathVariable Long campaignId,
            @RequestBody @Valid CampaignApplyRequest request) {
        return ApiResponse.ok(dojangdanService.apply(campaignId, request));
    }

    @GetMapping("/applications/me")
    public ApiResponse<List<MyCampaignApplicationResponse>> getMyApplications() {
        return ApiResponse.ok(dojangdanService.getMyApplications());
    }

    @PostMapping("/applications/{applicationId}/review")
    public ApiResponse<MyCampaignApplicationResponse> submitReview(
            @PathVariable Long applicationId,
            @RequestBody @Valid CampaignReviewSubmitRequest request) {
        return ApiResponse.ok(dojangdanService.submitReview(applicationId, request));
    }

    @GetMapping("/track-record/me")
    public ApiResponse<ReaderTrackRecordResponse> getMyTrackRecord() {
        return ApiResponse.ok(dojangdanService.getMyTrackRecord());
    }
}
