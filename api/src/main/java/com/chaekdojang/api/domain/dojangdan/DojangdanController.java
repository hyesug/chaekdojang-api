package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dojangdan")
@RequiredArgsConstructor
public class DojangdanController {

    private final DojangdanService dojangdanService;
    private final ReviewUsageConsentService consentService;

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
            @RequestBody @Valid CampaignApplyRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(
                dojangdanService.apply(campaignId, request, ClientIpUtils.getClientIp(httpRequest)));
    }

    @GetMapping("/applications/me")
    public ApiResponse<List<MyCampaignApplicationResponse>> getMyApplications() {
        return ApiResponse.ok(dojangdanService.getMyApplications());
    }

    @GetMapping("/applications/{applicationId}/submittable-reviews")
    public ApiResponse<List<SubmittableReviewResponse>> getSubmittableReviews(
            @PathVariable Long applicationId) {
        return ApiResponse.ok(dojangdanService.getSubmittableReviews(applicationId));
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

    @GetMapping("/applications/{applicationId}/consent")
    public ApiResponse<ReviewUsageConsentResponse> getConsent(@PathVariable Long applicationId) {
        return ApiResponse.ok(consentService.getMyConsent(applicationId));
    }

    @GetMapping("/applications/{applicationId}/consent/history")
    public ApiResponse<List<ReviewUsageConsentResponse>> getConsentHistory(
            @PathVariable Long applicationId) {
        return ApiResponse.ok(consentService.getMyConsentHistory(applicationId));
    }

    @PutMapping("/applications/{applicationId}/consent")
    public ApiResponse<ReviewUsageConsentResponse> updateConsent(
            @PathVariable Long applicationId,
            @RequestBody @Valid ConsentUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(consentService.updateMyConsent(
                applicationId, request, ClientIpUtils.getClientIp(httpRequest)));
    }

    @DeleteMapping("/applications/{applicationId}/consent")
    public ApiResponse<Void> revokeConsent(@PathVariable Long applicationId) {
        consentService.revokeMyConsent(applicationId);
        return ApiResponse.ok();
    }
}
