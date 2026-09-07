package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/dojangdan/manage")
@RequiredArgsConstructor
public class DojangdanManageController {

    private final DojangdanManageService manageService;
    private final CampaignExportService exportService;
    private final ProfileAudienceService audienceService;
    private final CampaignEbookService ebookService;

    @GetMapping("/profiles")
    public ApiResponse<List<ManagedProfileResponse>> getManagedProfiles() {
        return ApiResponse.ok(manageService.getManagedProfiles());
    }

    @PostMapping("/profiles/{profileId}/campaigns")
    public ApiResponse<ManageCampaignDetailResponse> createCampaign(
            @PathVariable Long profileId,
            @RequestBody @Valid CampaignCreateRequest request) {
        return ApiResponse.ok(manageService.createCampaign(profileId, request));
    }

    /** 관심 독자 집계. 개별 독자를 식별할 수 있는 정보는 내려주지 않는다. */
    @GetMapping("/profiles/{profileId}/audience")
    public ApiResponse<ProfileAudienceResponse> getAudience(@PathVariable Long profileId) {
        return ApiResponse.ok(audienceService.getAudience(profileId));
    }

    @GetMapping("/campaigns")
    public ApiResponse<List<CampaignSummaryResponse>> getMyCampaigns() {
        return ApiResponse.ok(manageService.getMyCampaigns());
    }

    @GetMapping("/campaigns/{campaignId}")
    public ApiResponse<ManageCampaignDetailResponse> getCampaign(@PathVariable Long campaignId) {
        return ApiResponse.ok(manageService.getCampaignDetail(campaignId));
    }

    @PatchMapping("/campaigns/{campaignId}")
    public ApiResponse<ManageCampaignDetailResponse> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody @Valid CampaignUpdateRequest request) {
        return ApiResponse.ok(manageService.updateCampaign(campaignId, request));
    }

    @PatchMapping("/campaigns/{campaignId}/status")
    public ApiResponse<ManageCampaignDetailResponse> updateStatus(
            @PathVariable Long campaignId,
            @RequestBody @Valid CampaignStatusUpdateRequest request) {
        return ApiResponse.ok(manageService.updateStatus(campaignId, request));
    }

    @GetMapping("/campaigns/{campaignId}/applications")
    public ApiResponse<List<CampaignApplicantResponse>> getApplicants(@PathVariable Long campaignId) {
        return ApiResponse.ok(manageService.getApplicants(campaignId));
    }

    @PostMapping("/campaigns/{campaignId}/select")
    public ApiResponse<List<CampaignApplicantResponse>> select(
            @PathVariable Long campaignId,
            @RequestBody @Valid CampaignSelectRequest request) {
        return ApiResponse.ok(manageService.select(campaignId, request));
    }

    @PostMapping("/campaigns/{campaignId}/ebook")
    public ApiResponse<EbookFileResponse> uploadEbook(
            @PathVariable Long campaignId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(ebookService.upload(campaignId, file));
    }

    @GetMapping("/campaigns/{campaignId}/ebook")
    public ApiResponse<EbookFileResponse> getEbook(@PathVariable Long campaignId) {
        return ApiResponse.ok(ebookService.getEbookFile(campaignId));
    }

    @GetMapping("/campaigns/{campaignId}/reviews")
    public ApiResponse<List<CampaignReviewSummaryResponse>> getCampaignReviews(
            @PathVariable Long campaignId) {
        return ApiResponse.ok(exportService.getCampaignReviews(campaignId));
    }

    @GetMapping("/campaigns/{campaignId}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "markdown") String format) {
        CampaignExportService.ExportFile file = exportService.export(campaignId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
