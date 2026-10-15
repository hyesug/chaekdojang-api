package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-credits")
@RequiredArgsConstructor
public class AiCreditAdminController {
    private final AiCreditService credits;
    @PostMapping("/{userId}/adjustments")
    public ApiResponse<Void> adjust(@PathVariable Long userId, @RequestBody Adjustment request) {
        credits.adjust(SecurityUtils.getCurrentUserId(), userId, request.amount(), request.description());
        return ApiResponse.ok(null);
    }
    @GetMapping("/users")
    public ApiResponse<AiCreditService.UserPage> users(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(credits.userList(q, page, size));
    }
    @GetMapping("/{userId}/usages")
    public ApiResponse<java.util.List<AiCreditService.UsageRow>> usages(@PathVariable Long userId, @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(credits.usageHistory(userId, limit));
    }
    @GetMapping("/{userId}")
    public ApiResponse<AiCreditService.AdminView> userCredits(@PathVariable Long userId) { return ApiResponse.ok(credits.adminView(userId)); }
    @GetMapping("/statistics")
    public ApiResponse<AiCreditService.Statistics> statistics() { return ApiResponse.ok(credits.statistics()); }
    public record Adjustment(@NotNull Integer amount, @NotBlank String description) {}
}
