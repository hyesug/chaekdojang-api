package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.officialprofile.dto.*;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공식 프로필", description = "작가·출판사·서점 공식 프로필 신청과 공개 조회")
@RestController
@RequiredArgsConstructor
public class OfficialProfileController {

    private final OfficialProfileService officialProfileService;

    @Operation(summary = "공식 프로필 신청", description = "로그인한 사용자가 작가·출판사·서점 프로필을 신청합니다.")
    @PostMapping("/api/profile-applications")
    public ApiResponse<OfficialProfileApplicationResponse> apply(
            @RequestBody @Valid OfficialProfileApplicationRequest request) {
        return ApiResponse.ok(officialProfileService.apply(request));
    }

    @Operation(summary = "내 공식 프로필 신청 목록", description = "로그인한 사용자의 공식 프로필 신청 내역을 반환합니다.")
    @GetMapping("/api/profile-applications/me")
    public ApiResponse<List<OfficialProfileApplicationResponse>> getMyApplications() {
        return ApiResponse.ok(officialProfileService.getMyApplications());
    }

    @Operation(summary = "공개 공식 프로필 목록", description = "활성화된 공식 프로필 목록을 반환합니다.")
    @GetMapping("/api/profiles")
    public ApiResponse<List<OfficialProfileResponse>> getProfiles() {
        return ApiResponse.ok(officialProfileService.getPublicProfiles());
    }

    @Operation(summary = "공개 공식 프로필 조회", description = "slug로 공개 공식 프로필을 조회합니다.")
    @GetMapping("/api/profiles/{slug}")
    public ApiResponse<OfficialProfileResponse> getProfile(@PathVariable String slug) {
        return ApiResponse.ok(officialProfileService.getPublicProfile(slug));
    }
}
