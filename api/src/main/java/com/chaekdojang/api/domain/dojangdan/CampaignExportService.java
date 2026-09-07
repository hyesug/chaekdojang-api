package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.dojangdan.dto.CampaignReviewSummaryResponse;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 캠페인 독후감 내보내기.
 * 활용 동의(consent_promotional)가 살아 있는 독후감만 본문을 내보낸다.
 */
@Service
@RequiredArgsConstructor
public class CampaignExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ANONYMOUS_NAME = "익명";

    private final ReviewCampaignApplicationRepository applicationRepository;
    private final ReviewUsageConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final CampaignAccessGuard accessGuard;
    private final AdminAuditLogService auditLogService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * 출판사 화면용 독후감 목록.
     * 미동의 건은 건수에는 포함하되 본문은 넘기지 않는다.
     */
    @Transactional(readOnly = true)
    public List<CampaignReviewSummaryResponse> getCampaignReviews(Long campaignId) {
        accessGuard.requireCampaignAccess(campaignId);
        return collectSubmitted(campaignId).stream()
                .map(entry -> new CampaignReviewSummaryResponse(
                        entry.application().getId(),
                        entry.review().getId(),
                        entry.displayName(),
                        entry.promotional(),
                        entry.excerpt(),
                        entry.review().getContent().length(),
                        entry.application().getSubmittedAt(),
                        reviewUrl(entry.review().getId()),
                        entry.promotional() ? entry.review().getContent() : null
                ))
                .toList();
    }

    @Transactional
    public ExportFile export(Long campaignId, String format) {
        ReviewCampaign campaign = accessGuard.requireCampaignAccess(campaignId);
        List<SubmittedReview> exportable = collectSubmitted(campaignId).stream()
                .filter(SubmittedReview::promotional)
                .toList();

        ExportFile file = switch (format == null ? "" : format.toLowerCase()) {
            case "markdown", "md" -> markdown(campaign, exportable);
            case "csv" -> csv(campaign, exportable);
            case "zip", "txt" -> zip(campaign, exportable);
            default -> throw new CustomException(ErrorCode.EXPORT_FORMAT_NOT_SUPPORTED);
        };

        auditLogService.record(
                userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null),
                "DOJANGDAN_REVIEW_EXPORT",
                "REVIEW_CAMPAIGN",
                campaignId,
                "캠페인 '%s' 독후감 %d건을 %s 형식으로 내보냄"
                        .formatted(campaign.getTitle(), exportable.size(), format)
        );
        return file;
    }

    private List<SubmittedReview> collectSubmitted(Long campaignId) {
        List<ReviewCampaignApplication> submitted = applicationRepository
                .findByCampaignIdAndStatusIn(campaignId, List.of(CampaignApplicationStatus.SUBMITTED))
                .stream()
                .filter(application -> application.getReview() != null
                        && application.getReview().getDeletedAt() == null)
                .toList();
        if (submitted.isEmpty()) return List.of();

        Map<Long, ReviewUsageConsent> consents = consentRepository
                .findByApplicationIdInAndRevokedAtIsNull(
                        submitted.stream().map(ReviewCampaignApplication::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        consent -> consent.getApplication().getId(), Function.identity()));

        return submitted.stream()
                .map(application -> {
                    ReviewUsageConsent consent = consents.get(application.getId());
                    boolean anonymous = consent != null
                            && consent.getDisplayNameType() == ConsentDisplayNameType.ANONYMOUS;
                    return new SubmittedReview(
                            application,
                            application.getReview(),
                            anonymous ? ANONYMOUS_NAME : application.getUser().getNickname(),
                            consent != null && consent.allowsPromotionalUse(),
                            consent != null && consent.isConsentExcerpt()
                    );
                })
                .toList();
    }

    private ExportFile markdown(ReviewCampaign campaign, List<SubmittedReview> reviews) {
        StringBuilder out = new StringBuilder();
        out.append("# ").append(campaign.getTitle()).append("\n\n");
        out.append("- 도서: ").append(campaign.getBook().getTitle())
                .append(" · ").append(campaign.getBook().getAuthor()).append("\n");
        out.append("- 주최: ").append(campaign.getProfile().getDisplayName()).append("\n");
        out.append("- 내보낸 날짜: ").append(LocalDate.now().format(DATE)).append("\n");
        out.append("- 활용 동의 독후감: ").append(reviews.size()).append("건\n\n");
        out.append("> 아래 독후감은 작성자가 홍보 활용에 동의한 것입니다. ")
                .append("발췌·편집 허용 여부는 각 항목에 표시했습니다.\n\n");

        for (SubmittedReview entry : reviews) {
            out.append("---\n\n");
            out.append("## ").append(entry.displayName()).append("\n\n");
            out.append("- 작성일: ")
                    .append(entry.review().getCreatedAt().toLocalDate().format(DATE)).append("\n");
            out.append("- 글자수: ").append(entry.review().getContent().length()).append("자\n");
            out.append("- 발췌·편집: ").append(entry.excerpt() ? "허용" : "불가").append("\n");
            out.append("- 원문: ").append(reviewUrl(entry.review().getId())).append("\n\n");
            out.append(entry.review().getContent()).append("\n\n");
        }

        return new ExportFile(
                fileName(campaign, "md"),
                "text/markdown; charset=UTF-8",
                out.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private ExportFile csv(ReviewCampaign campaign, List<SubmittedReview> reviews) {
        StringBuilder out = new StringBuilder();
        // 엑셀에서 한글이 깨지지 않도록 BOM을 붙인다.
        out.append('﻿');
        out.append("표기명,작성일,글자수,발췌·편집 허용,독후감 URL,본문\n");
        for (SubmittedReview entry : reviews) {
            Review review = entry.review();
            out.append(csvCell(entry.displayName())).append(',')
                    .append(csvCell(review.getCreatedAt().toLocalDate().format(DATE))).append(',')
                    .append(review.getContent().length()).append(',')
                    .append(csvCell(entry.excerpt() ? "허용" : "불가")).append(',')
                    .append(csvCell(reviewUrl(review.getId()))).append(',')
                    .append(csvCell(review.getContent())).append('\n');
        }

        return new ExportFile(
                fileName(campaign, "csv"),
                "text/csv; charset=UTF-8",
                out.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private ExportFile zip(ReviewCampaign campaign, List<SubmittedReview> reviews) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            int index = 1;
            for (SubmittedReview entry : reviews) {
                String name = "%02d_%s.txt".formatted(index++, safeFileName(entry.displayName()));
                zip.putNextEntry(new ZipEntry(name));

                String body = """
                        %s
                        작성일: %s
                        글자수: %d자
                        발췌·편집: %s
                        원문: %s

                        %s
                        """.formatted(
                        entry.displayName(),
                        entry.review().getCreatedAt().toLocalDate().format(DATE),
                        entry.review().getContent().length(),
                        entry.excerpt() ? "허용" : "불가",
                        reviewUrl(entry.review().getId()),
                        entry.review().getContent()
                );
                zip.write(body.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("독후감 ZIP을 만들지 못했습니다.", e);
        }

        return new ExportFile(fileName(campaign, "zip"), "application/zip", buffer.toByteArray());
    }

    private String csvCell(String value) {
        if (value == null) return "";
        // CSV 따옴표는 수식 실행을 막지 않는다. 앞쪽 공백/제어문자 뒤의 수식도 텍스트로 취급한다.
        String stripped = value.stripLeading().replaceFirst("^[\\p{Cc}\\p{Cf}\\s]+", "");
        if ((!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0)
                || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) {
            value = "'" + value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String safeFileName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return cleaned.isBlank() ? "review" : cleaned;
    }

    private String fileName(ReviewCampaign campaign, String extension) {
        return "책도장단_%d_%s.%s".formatted(
                campaign.getId(), LocalDate.now().format(DATE), extension);
    }

    private String reviewUrl(Long reviewId) {
        String base = frontendUrl != null && frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        return base + "/reviews/" + reviewId;
    }

    private record SubmittedReview(
            ReviewCampaignApplication application,
            Review review,
            String displayName,
            boolean promotional,
            boolean excerpt
    ) {
    }

    public record ExportFile(String fileName, String contentType, byte[] content) {
    }
}
