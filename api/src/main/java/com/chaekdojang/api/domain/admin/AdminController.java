package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.admin.dto.AccessLogResponse;
import com.chaekdojang.api.domain.admin.dto.AdminAuditLogResponse;
import com.chaekdojang.api.domain.admin.dto.AdminReviewResponse;
import com.chaekdojang.api.domain.admin.dto.AdminUserResponse;
import com.chaekdojang.api.domain.admin.dto.BookReviewStatResponse;
import com.chaekdojang.api.domain.admin.dto.ErrorLogResponse;
import com.chaekdojang.api.domain.admin.dto.MetricEventResponse;
import com.chaekdojang.api.domain.inquiry.dto.InquiryResponse;
import com.chaekdojang.api.domain.user.UserRole;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── 회원 관리 ──────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getUsers(SecurityUtils.getCurrentUserId(), pageable)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> setRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        UserRole role = UserRole.valueOf(body.get("role").toUpperCase());
        adminService.setRole(SecurityUtils.getCurrentUserId(), id, role);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── 독후감 관리 ─────────────────────────────────────────
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Page<AdminReviewResponse>>> getReviews(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getReviews(SecurityUtils.getCurrentUserId(), author, title, pageable)));
    }

    @PatchMapping("/reviews/{id}/hidden")
    public ResponseEntity<ApiResponse<Void>> setHidden(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        adminService.setHidden(SecurityUtils.getCurrentUserId(), id, body.get("hidden"));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/reviews/stats")
    public ResponseEntity<ApiResponse<List<BookReviewStatResponse>>> getBookStats() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getBookStats(SecurityUtils.getCurrentUserId())));
    }

    // ── 문의 관리 ──────────────────────────────────────────
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getInquiries(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getInquiries(SecurityUtils.getCurrentUserId(), pageable)));
    }

    @GetMapping("/inquiries/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> getInquiryDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getInquiryDetail(SecurityUtils.getCurrentUserId(), id)));
    }

    // ── 접속 기록 ──────────────────────────────────────────
    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<Page<AccessLogResponse>>> getAccessLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String statusGroup,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getAccessLogs(SecurityUtils.getCurrentUserId(), q, method, statusGroup, pageable)));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Page<MetricEventResponse>>> getMetricEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String userType,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getMetricEvents(SecurityUtils.getCurrentUserId(), q, eventType, userType, pageable)));
    }

    @GetMapping("/error-logs")
    public ResponseEntity<ApiResponse<Page<ErrorLogResponse>>> getErrorLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String statusGroup,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getErrorLogs(SecurityUtils.getCurrentUserId(), q, level, statusGroup, pageable)));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getAuditLogs(SecurityUtils.getCurrentUserId(), q, action, targetType, pageable)));
    }

    @PostMapping("/inquiries/{id}/comments")
    public ResponseEntity<ApiResponse<InquiryResponse>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.addComment(SecurityUtils.getCurrentUserId(), id, body.get("content"))));
    }
}
