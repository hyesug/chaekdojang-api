package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai-credit-orders")
@RequiredArgsConstructor
public class AiCreditOrderController {
    private final AiCreditOrderService orders;

    @GetMapping("/products")
    public ApiResponse<?> products() { return ApiResponse.ok(orders.products()); }

    @PostMapping
    public ApiResponse<AiCreditOrderService.Order> create(@RequestBody CreateRequest request) { return ApiResponse.ok(orders.create(SecurityUtils.getCurrentUserId(), request.product())); }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<AiCreditOrderService.Order> complete(@PathVariable UUID orderId) { return ApiResponse.ok(orders.complete(SecurityUtils.getCurrentUserId(), orderId)); }

    public record CreateRequest(AiCreditProduct product) {}
}
