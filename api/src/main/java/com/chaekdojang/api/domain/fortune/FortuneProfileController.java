package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fortune-profiles")
@RequiredArgsConstructor
public class FortuneProfileController {

    private final FortuneProfileService fortuneProfileService;

    @GetMapping("/me")
    public ApiResponse<FortuneProfileResponse> getMine() {
        return ApiResponse.ok(fortuneProfileService.getMine(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<FortuneProfileResponse> saveMine(@Valid @RequestBody FortuneProfileRequest request) {
        return ApiResponse.ok(fortuneProfileService.saveMine(SecurityUtils.getCurrentUserId(), request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMine() {
        fortuneProfileService.deleteMine(SecurityUtils.getCurrentUserId());
        return ApiResponse.ok();
    }
}
