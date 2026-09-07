package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/dojangdan")
@RequiredArgsConstructor
public class DojangdanController {

    private final DojangdanService dojangdanService;
    private final ReviewUsageConsentService consentService;
    private final ProfileFollowIntentService followIntentService;
    private final CampaignEbookService ebookService;

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

    /** 미선정 통보 안에서 "다음 책 소식 받기"를 다시 확인할 때 쓴다. */
    @PutMapping("/applications/{applicationId}/follow-intent")
    public ApiResponse<Void> updateFollowIntent(
            @PathVariable Long applicationId,
            @RequestBody @Valid FollowIntentUpdateRequest request) {
        followIntentService.updateFromApplication(applicationId, request.subscribe());
        return ApiResponse.ok();
    }

    @GetMapping("/follow-intents/me")
    public ApiResponse<List<MyFollowIntentResponse>> getMyFollowIntents() {
        return ApiResponse.ok(followIntentService.getMyIntents());
    }

    /** 수신 거부 1클릭 해제 */
    @DeleteMapping("/follow-intents/{profileId}")
    public ApiResponse<Void> unsubscribeFollowIntent(@PathVariable Long profileId) {
        followIntentService.unsubscribe(profileId);
        return ApiResponse.ok();
    }

    @GetMapping("/applications/{applicationId}/ebook")
    public ApiResponse<MyEbookAccessResponse> getEbookAccess(@PathVariable Long applicationId) {
        return ApiResponse.ok(ebookService.getMyAccess(applicationId));
    }

    /**
     * 워터마크가 박힌 전자책을 내려받는다.
     * 파일 주소를 밖으로 내보내지 않고 서버가 직접 흘려보내, 매 요청마다 권한을 다시 확인한다.
     */
    @GetMapping("/applications/{applicationId}/ebook/download")
    public ResponseEntity<byte[]> downloadEbook(
            @PathVariable Long applicationId,
            HttpServletRequest httpRequest) {
        CampaignEbookService.EbookDownload download =
                ebookService.download(applicationId, ClientIpUtils.getClientIp(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PDF)
                .body(download.content());
    }

    /** 서평단 중도 포기. 전자책 열람 권한도 즉시 회수한다. */
    @PostMapping("/applications/{applicationId}/drop")
    public ApiResponse<Void> dropOut(@PathVariable Long applicationId) {
        ebookService.dropOut(applicationId);
        return ApiResponse.ok();
    }
}
