package com.chaekdojang.api.domain.commerce;

import com.chaekdojang.api.domain.commerce.dto.PurchaseLinkAddRequest;
import com.chaekdojang.api.domain.commerce.dto.PurchaseLinkResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "구매 링크", description = "도서 구매 링크 조회·등록 (쿠팡파트너스·교보)")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class PurchaseLinkController {

    private final PurchaseLinkService purchaseLinkService;

    @Operation(summary = "도서 구매 링크 조회", description = "특정 도서의 쿠팡파트너스·교보 구매 링크 목록을 반환합니다.")
    @GetMapping("/{bookId}/purchase-links")
    public ApiResponse<List<PurchaseLinkResponse>> getLinks(@PathVariable Long bookId) {
        return ApiResponse.ok(purchaseLinkService.getLinks(bookId));
    }

    @Operation(summary = "구매 링크 등록", description = "도서에 구매 링크를 추가합니다. JWT 필요.")
    @PostMapping("/purchase-links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PurchaseLinkResponse> addLink(
            @RequestBody @Valid PurchaseLinkAddRequest request) {
        return ApiResponse.ok(purchaseLinkService.addLink(request));
    }

    @Operation(summary = "구매 링크 클릭수 기록", description = "구매 링크 클릭을 기록합니다. 인증 불필요.")
    @PostMapping("/purchase-links/{linkId}/clicks")
    public ApiResponse<Void> recordClick(@PathVariable Long linkId) {
        purchaseLinkService.recordClick(linkId);
        return ApiResponse.ok(null);
    }
}
