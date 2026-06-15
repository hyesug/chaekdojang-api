package com.chaekdojang.api.domain.admin;

import com.chaekdojang.api.domain.accesslog.AccessLog;
import com.chaekdojang.api.domain.accesslog.AccessLogService;
import com.chaekdojang.api.domain.admin.dto.*;
import com.chaekdojang.api.domain.inquiry.Inquiry;
import com.chaekdojang.api.domain.inquiry.InquiryComment;
import com.chaekdojang.api.domain.inquiry.InquiryCommentRepository;
import com.chaekdojang.api.domain.inquiry.InquiryRepository;
import com.chaekdojang.api.domain.inquiry.dto.InquiryResponse;
import com.chaekdojang.api.domain.metrics.MetricEventRepository;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.domain.user.UserRole;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final AccessLogService accessLogService;
    private final MetricEventRepository metricEventRepository;

    // ── 권한 검증 ──────────────────────────────────────────
    private User assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.isAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    private User assertSuperAdmin(Long userId) {
        User user = assertAdmin(userId);
        if (!user.isSuperAdmin()) throw new CustomException(ErrorCode.FORBIDDEN);
        return user;
    }

    // ── 회원 관리 ──────────────────────────────────────────
    public Page<AdminUserResponse> getUsers(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return userRepository.findAllByDeletedAtIsNull(pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional
    public void setRole(Long adminId, Long targetUserId, UserRole role) {
        assertSuperAdmin(adminId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (target.isSuperAdmin()) throw new CustomException(ErrorCode.FORBIDDEN); // 슈퍼 관리자는 변경 불가
        if (role == UserRole.ADMIN) target.promoteToAdmin();
        else target.demoteToUser();
    }

    // ── 독후감 관리 ─────────────────────────────────────────
    public Page<AdminReviewResponse> getReviews(Long adminId, String author, String title, Pageable pageable) {
        assertAdmin(adminId);
        String a = (author != null && !author.isBlank()) ? author.trim() : null;
        String t = (title != null && !title.isBlank()) ? title.trim() : null;
        return reviewRepository.findAllByDeletedAtIsNullWithSearch(a, t, pageable)
                .map(AdminReviewResponse::from);
    }

    @Transactional
    public void setHidden(Long adminId, Long reviewId, boolean hidden) {
        assertAdmin(adminId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (hidden) review.hide(); else review.unhide();
    }

    public List<BookReviewStatResponse> getBookStats(Long adminId) {
        assertAdmin(adminId);
        return reviewRepository.findBookReviewStats();
    }

    // ── 문의 관리 ──────────────────────────────────────────
    public Page<InquiryResponse> getInquiries(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return inquiryRepository.findAllByDeletedAtIsNull(pageable)
                .map(InquiryResponse::summary);
    }

    public InquiryResponse getInquiryDetail(Long adminId, Long inquiryId) {
        assertAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return InquiryResponse.from(inquiry);
    }

    // ── 접속 기록 ──────────────────────────────────────────
    public Page<AccessLogResponse> getAccessLogs(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return accessLogService.getAll(pageable).map(AccessLogResponse::from);
    }

    public Page<MetricEventResponse> getMetricEvents(Long adminId, Pageable pageable) {
        assertAdmin(adminId);
        return metricEventRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(MetricEventResponse::from);
    }

    @Transactional
    public InquiryResponse addComment(Long adminId, Long inquiryId, String content) {
        User admin = assertAdmin(adminId);
        Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        inquiryCommentRepository.save(InquiryComment.create(inquiry, admin, content));
        // 변경감지를 위해 다시 조회
        return InquiryResponse.from(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId).get());
    }
}
