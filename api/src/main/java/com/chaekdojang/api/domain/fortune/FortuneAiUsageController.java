package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fortune-ai")
@RequiredArgsConstructor
public class FortuneAiUsageController {

    private final AiCreditService aiCreditService;

    @PostMapping("/reservations")
    public ApiResponse<AiCreditService.Reservation> reserve(@RequestHeader("X-Idempotency-Key") java.util.UUID requestId, @RequestBody ReserveRequest request) {
        return ApiResponse.ok(aiCreditService.reserve(SecurityUtils.getCurrentUserId(), requestId, request.tier()));
    }

    @PostMapping("/reservations/{requestId}/complete")
    public ApiResponse<Void> complete(@org.springframework.web.bind.annotation.PathVariable java.util.UUID requestId, @RequestBody CompleteRequest request) {
        aiCreditService.complete(SecurityUtils.getCurrentUserId(), requestId, new AiCreditService.Usage(request.model(), request.inputTokens(), request.outputTokens(), request.cacheReadTokens(), request.cacheWriteTokens(), request.durationMs(), request.requestedAt()));
        return ApiResponse.ok(null);
    }

    @PostMapping("/reservations/{requestId}/refund")
    public ApiResponse<Void> refund(@org.springframework.web.bind.annotation.PathVariable java.util.UUID requestId, @RequestBody RefundRequest request) { aiCreditService.refund(SecurityUtils.getCurrentUserId(), requestId, request.errorType()); return ApiResponse.ok(null); }

    @GetMapping("/credits/me")
    public ApiResponse<CreditResponse> credits() { Long id=SecurityUtils.getCurrentUserId(); return ApiResponse.ok(new CreditResponse(aiCreditService.balanceView(id), aiCreditService.history(id))); }

    public record CompleteRequest(String model, int inputTokens, int outputTokens, int cacheReadTokens, int cacheWriteTokens, long durationMs, java.time.LocalDateTime requestedAt) {}
    public record ReserveRequest(FortuneAiModelTier tier) {}
    public record RefundRequest(String errorType) {}
    public record CreditResponse(AiCreditService.Balance balance, java.util.List<AiCreditService.History> recentHistory) {}
}
