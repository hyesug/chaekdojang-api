package com.chaekdojang.api.domain.recap;

import com.chaekdojang.api.domain.recap.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-recaps")
@RequiredArgsConstructor
public class ReadingRecapController {
    private final ReadingRecapService recapService;

    @GetMapping("/preference")
    public ApiResponse<ReadingRecapPreferenceResponse> getPreference() {
        return ApiResponse.ok(recapService.getPreference());
    }

    @PutMapping("/preference")
    public ApiResponse<ReadingRecapPreferenceResponse> subscribe(
            @RequestBody @Valid ReadingRecapPreferenceRequest request) {
        return ApiResponse.ok(recapService.subscribe(request));
    }

    @DeleteMapping("/preference")
    public ApiResponse<Void> unsubscribe() {
        recapService.unsubscribe();
        return ApiResponse.ok(null);
    }

    @GetMapping("/issues")
    public ApiResponse<List<ReadingRecapIssueResponse>> getIssues() {
        return ApiResponse.ok(recapService.getIssues());
    }

    @PostMapping("/issues/current")
    public ApiResponse<ReadingRecapIssueResponse> generateCurrentIssue() {
        return ApiResponse.ok(recapService.generateCurrentIssue());
    }

    @PatchMapping("/issues/{issueId}/read")
    public ApiResponse<ReadingRecapIssueResponse> markRead(@PathVariable Long issueId) {
        return ApiResponse.ok(recapService.markRead(issueId));
    }
}
