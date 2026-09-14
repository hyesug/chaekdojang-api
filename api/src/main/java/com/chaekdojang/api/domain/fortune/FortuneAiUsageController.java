package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fortune-ai")
@RequiredArgsConstructor
public class FortuneAiUsageController {

    private final FortuneAiUsageService fortuneAiUsageService;

    @PostMapping("/reservations")
    public ApiResponse<FortuneAiUsageService.ReservationResponse> reserve() {
        return ApiResponse.ok(fortuneAiUsageService.reserve(SecurityUtils.getCurrentUserId()));
    }
}
