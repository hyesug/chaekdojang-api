package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.officialprofile.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOfficialProfileController {

    private final OfficialProfileService officialProfileService;

    @GetMapping("/profile-applications")
    public ApiResponse<Page<OfficialProfileApplicationResponse>> getApplications(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(officialProfileService.getApplications(pageable));
    }

    @PostMapping("/profile-applications/{id}/approve")
    public ApiResponse<OfficialProfileApplicationResponse> approve(
            @PathVariable Long id,
            @RequestBody @Valid OfficialProfileReviewRequest request) {
        return ApiResponse.ok(officialProfileService.approveApplication(SecurityUtils.getCurrentUserId(), id, request));
    }

    @PostMapping("/profile-applications/{id}/reject")
    public ApiResponse<OfficialProfileApplicationResponse> reject(
            @PathVariable Long id,
            @RequestBody @Valid OfficialProfileReviewRequest request) {
        return ApiResponse.ok(officialProfileService.rejectApplication(SecurityUtils.getCurrentUserId(), id, request));
    }

    @GetMapping("/profiles")
    public ApiResponse<List<OfficialProfileResponse>> getProfiles() {
        return ApiResponse.ok(officialProfileService.getProfiles());
    }

    @PutMapping("/profiles/{id}")
    public ApiResponse<OfficialProfileResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody @Valid OfficialProfileUpdateRequest request) {
        return ApiResponse.ok(officialProfileService.updateProfile(SecurityUtils.getCurrentUserId(), id, request));
    }

    @PostMapping("/profiles/{id}/books")
    public ApiResponse<OfficialProfileResponse> addBook(
            @PathVariable Long id,
            @RequestBody @Valid OfficialProfileBookAddRequest request) {
        return ApiResponse.ok(officialProfileService.addBook(SecurityUtils.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/profiles/{id}/books/{bookId}")
    public ApiResponse<OfficialProfileResponse> removeBook(
            @PathVariable Long id,
            @PathVariable Long bookId) {
        return ApiResponse.ok(officialProfileService.removeBook(SecurityUtils.getCurrentUserId(), id, bookId));
    }
}
