package com.chaekdojang.api.domain.fortune.feedback;

import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest;
import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.Summary;
import com.chaekdojang.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 운세 결과 피드백 — 누구나 남길 수 있고(로그인 불필요), 관리자는 칸별 집계와 최근 글을 본다.
 */
@RestController
@RequiredArgsConstructor
public class FortuneFeedbackController {

    private final FortuneFeedbackService service;

    @Value("${app.fortune-feedback.export-token:}")
    private String exportToken;

    @PostMapping("/api/fortune/feedback")
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody CreateRequest req) {
        service.create(req);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 매주 도는 사전 자동 수정 작업이 읽는 내보내기 — 관리자 로그인 대신 전용 토큰으로만 연다(읽기 전용).
     * 토큰이 설정되지 않았으면 언제나 막는다.
     */
    @GetMapping("/api/internal/fortune-feedback")
    public ResponseEntity<ApiResponse<Summary>> export(
            @RequestHeader(value = "X-Fortune-Feedback-Token", required = false) String token,
            @RequestParam(defaultValue = "500") int limit) {
        if (exportToken == null || exportToken.isBlank() || token == null
                || !MessageDigest.isEqual(exportToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(ApiResponse.ok(service.summary(limit)));
    }

    @GetMapping("/api/admin/fortune-feedback")
    public ResponseEntity<ApiResponse<Summary>> summary(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(service.summary(limit)));
    }
}
