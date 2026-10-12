package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ── 여러 사람 저장 (본인 + 가족·지인) ──

    @GetMapping
    public ApiResponse<List<FortuneProfileResponse>> list() {
        return ApiResponse.ok(fortuneProfileService.list(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ApiResponse<FortuneProfileResponse> create(@Valid @RequestBody FortuneProfileRequest request) {
        return ApiResponse.ok(fortuneProfileService.create(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<FortuneProfileResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody FortuneProfileRequest request) {
        return ApiResponse.ok(fortuneProfileService.update(SecurityUtils.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fortuneProfileService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.ok();
    }
}
