package com.chaekdojang.api.domain.fortune.feedback;

import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest;
import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.Summary;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 운세 결과 피드백 — 누구나 남길 수 있고(로그인 불필요), 관리자는 칸별 집계와 최근 글을 본다.
 */
@RestController
@RequiredArgsConstructor
public class FortuneFeedbackController {

    private final FortuneFeedbackService service;

    @PostMapping("/api/fortune/feedback")
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateRequest req) {
        service.create(req);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/api/admin/fortune-feedback")
    public ResponseEntity<ApiResponse<Summary>> summary(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(service.summary(limit)));
    }
}
