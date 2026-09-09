package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.dojangdan.dto.EbookFileResponse;
import com.chaekdojang.api.domain.dojangdan.dto.MyEbookAccessResponse;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * PDF 서평단.
 * 실물 배송 없이 캠페인을 열 수 있게 해서, 인쇄비·택배비가 부담인 독립작가도 서평단을 운영할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignEbookService {

    private static final long MAX_EBOOK_BYTES = 50L * 1024 * 1024;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CampaignEbookFileRepository ebookFileRepository;
    private final EbookAccessGrantRepository grantRepository;
    private final ReviewCampaignApplicationRepository applicationRepository;
    private final EbookStorageService storageService;
    private final EbookWatermarkService watermarkService;
    private final CampaignAccessGuard accessGuard;
    private final ReviewCampaignRepository campaignRepository;
    private final EbookOpenLogRepository openLogRepository;

    @Transactional
    public EbookFileResponse upload(Long campaignId, MultipartFile file) {
        ReviewCampaign campaign = accessGuard.requireCampaignWriteAccess(campaignId);
        if (!campaign.getDeliveryType().isEbook()) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_EBOOK);
        }
        byte[] content = readPdf(file);

        String storageKey = storageService.putOriginal(content, "pdf");
        Integer pageCount = watermarkService.readPageCount(content);
        String originalName = file.getOriginalFilename() == null
                ? "ebook.pdf"
                : file.getOriginalFilename();

        CampaignEbookFile ebookFile = ebookFileRepository.findByCampaignId(campaignId)
                .orElse(null);
        if (ebookFile == null) {
            ebookFile = ebookFileRepository.save(CampaignEbookFile.builder()
                    .campaign(campaign)
                    .storageKey(storageKey)
                    .originalFilename(originalName)
                    .mimeType("application/pdf")
                    .byteSize(content.length)
                    .pageCount(pageCount)
                    .build());
        } else {
            ebookFile.replace(storageKey, originalName, "application/pdf", content.length, pageCount);
            grantRepository.findByApplicationCampaignId(campaignId)
                    .forEach(EbookAccessGrant::invalidateWatermark);
        }
        return EbookFileResponse.from(ebookFile);
    }

    @Transactional(readOnly = true)
    public EbookFileResponse getEbookFile(Long campaignId) {
        accessGuard.requireCampaignAccess(campaignId);
        return ebookFileRepository.findByCampaignId(campaignId)
                .map(EbookFileResponse::from)
                .orElse(null);
    }

    /** 선정 확정 시 선정자에게 열람 권한을 만든다. 이미 있으면 만료일만 맞춘다. */
    @Transactional
    public void grantAccess(ReviewCampaignApplication application) {
        ReviewCampaign campaign = application.getCampaign();
        if (!campaign.getDeliveryType().isEbook()) return;

        grantRepository.findByApplicationId(application.getId())
                .ifPresentOrElse(
                        grant -> grant.extendUntil(campaign.ebookExpiresAt()),
                        () -> grantRepository.save(EbookAccessGrant.builder()
                                .application(application)
                                .expiresAt(campaign.ebookExpiresAt())
                                .build())
                );
    }

    @Transactional(readOnly = true)
    public MyEbookAccessResponse getMyAccess(Long applicationId) {
        ReviewCampaignApplication application = requireMyApplication(applicationId);
        if (!application.getCampaign().getDeliveryType().isEbook()) return null;

        return grantRepository.findByApplicationId(applicationId)
                .map(grant -> MyEbookAccessResponse.of(
                        grant,
                        ebookFileRepository.findByCampaignId(application.getCampaign().getId())
                                .orElse(null),
                        LocalDateTime.now(KST)))
                .orElse(null);
    }

    /**
     * 워터마크가 박힌 파일을 내려준다.
     *
     * 서명된 외부 URL 대신 서버가 직접 흘려보낸다. 매 요청마다 권한·만료·철회를 다시 확인할 수 있고,
     * 파일 주소 자체가 밖으로 나가지 않기 때문이다.
     */
    @Transactional
    public EbookDownload download(Long applicationId, String ip) {
        ReviewCampaignApplication application = requireMyApplication(applicationId);
        // 교체/기한 변경은 배타 잠금, 다운로드는 공유 잠금으로 동시에 여러 독자가 읽을 수 있다.
        ReviewCampaign campaign = campaignRepository.findForRead(application.getCampaign().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND));
        if (!campaign.getDeliveryType().isEbook()) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_EBOOK);
        }

        EbookAccessGrant grant = grantRepository.findForUpdate(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.EBOOK_ACCESS_DENIED));
        if (grant.isRevoked()) {
            throw new CustomException(ErrorCode.EBOOK_ACCESS_REVOKED);
        }
        if (grant.isExpired(LocalDateTime.now(KST))) {
            throw new CustomException(ErrorCode.EBOOK_ACCESS_EXPIRED);
        }

        CampaignEbookFile ebookFile = ebookFileRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.EBOOK_FILE_NOT_UPLOADED));

        byte[] content = resolveWatermarked(grant, ebookFile, application);
        grant.recordOpen(ip);
        openLogRepository.save(new EbookOpenLog(grant, ip, ebookFile.getStorageKey()));

        return new EbookDownload(downloadFileName(campaign), content);
    }

    /** 중도 포기. 권한을 즉시 회수한다. */
    @Transactional
    public void dropOut(Long applicationId) {
        ReviewCampaignApplication application = requireMyApplication(applicationId);
        campaignRepository.findForRead(application.getCampaign().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND));
        if (application.getStatus() != CampaignApplicationStatus.SELECTED) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_SELECTED);
        }
        application.drop();
        grantRepository.findForUpdate(applicationId).ifPresent(EbookAccessGrant::revoke);
    }

    /** 캠페인 수정의 배타 잠금 안에서 기존 권한의 만료일도 함께 맞춘다. */
    @Transactional
    public void syncExpiration(ReviewCampaign campaign) {
        grantRepository.findByApplicationCampaignId(campaign.getId()).stream()
                .filter(grant -> !grant.isRevoked())
                .forEach(grant -> grant.extendUntil(campaign.ebookExpiresAt()));
    }

    /**
     * 워터마크 파일은 한 번 만들어 두고 다시 쓴다.
     * 선정 시점에 미리 만들어두면 더 빠르지만, 지금은 첫 열람 때 만들고 캐시하는 쪽이 단순하다.
     */
    private byte[] resolveWatermarked(EbookAccessGrant grant, CampaignEbookFile ebookFile,
                                      ReviewCampaignApplication application) {
        if (grant.getWatermarkStatus() == WatermarkStatus.READY
                && grant.getWatermarkedStorageKey() != null) {
            try {
                return storageService.get(grant.getWatermarkedStorageKey());
            } catch (CustomException e) {
                // 저장소 미설정 같은 환경 문제는 다시 만들어도 소용없다. 그대로 알린다.
                throw e;
            } catch (RuntimeException e) {
                log.warn("워터마크 파일을 읽지 못해 다시 만듭니다. grantId={}", grant.getId(), e);
            }
        }

        try {
            byte[] source = storageService.get(ebookFile.getStorageKey());
            byte[] watermarked = watermarkService.watermark(
                    source,
                    application.getCampaign().getId(),
                    application.getUser().getId(),
                    application.getUser().getNickname());
            grant.markWatermarkReady(storageService.putWatermarked(grant.getId(), watermarked));
            return watermarked;
        } catch (CustomException e) {
            // 파일이 깨진 게 아니라 설정이 없는 것이므로 권한을 실패로 낙인찍지 않는다.
            throw e;
        } catch (RuntimeException e) {
            grant.markWatermarkFailed();
            log.error("전자책 워터마크 생성 실패. grantId={}", grant.getId(), e);
            throw new CustomException(ErrorCode.EBOOK_WATERMARK_FAILED);
        }
    }

    private byte[] readPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EBOOK_FILE_INVALID);
        }
        if (file.getSize() > MAX_EBOOK_BYTES) {
            throw new CustomException(ErrorCode.EBOOK_FILE_TOO_LARGE);
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.EBOOK_FILE_INVALID);
        }
        // 확장자·Content-Type은 속일 수 있으므로 PDF 시그니처를 직접 확인한다.
        if (content.length < 5
                || content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F') {
            throw new CustomException(ErrorCode.EBOOK_FILE_INVALID);
        }
        return content;
    }

    private String downloadFileName(ReviewCampaign campaign) {
        return "책도장단_%d_%s.pdf".formatted(campaign.getId(), campaign.getBook().getTitle())
                .replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private ReviewCampaignApplication requireMyApplication(Long applicationId) {
        ReviewCampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CAMPAIGN_APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(SecurityUtils.getCurrentUserId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return application;
    }

    /** 출판사 화면에서 열람 현황을 볼 때 쓴다. */
    @Transactional(readOnly = true)
    public List<EbookAccessGrant> getGrants(List<Long> applicationIds) {
        return grantRepository.findByApplicationIdIn(applicationIds);
    }

    public record EbookDownload(String fileName, byte[] content) {
    }
}
