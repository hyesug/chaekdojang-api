package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignEbookServiceTest {

    private static final Long READER_ID = 42L;
    private static final Long APPLICATION_ID = 7L;
    private static final Long CAMPAIGN_ID = 100L;

    @Mock CampaignEbookFileRepository ebookFileRepository;
    @Mock EbookAccessGrantRepository grantRepository;
    @Mock ReviewCampaignApplicationRepository applicationRepository;
    @Mock EbookStorageService storageService;
    @Mock EbookWatermarkService watermarkService;
    @Mock CampaignAccessGuard accessGuard;
    @Mock ReviewCampaignRepository campaignRepository;
    @Mock EbookOpenLogRepository openLogRepository;
    @InjectMocks CampaignEbookService service;

    private ReviewCampaign campaign;
    private ReviewCampaignApplication application;

    @BeforeEach
    void setUp() {
        campaign = campaign(CampaignDeliveryType.PDF);
        application = selectedApplication();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(READER_ID, null, List.of()));
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        org.mockito.Mockito.lenient().when(campaignRepository.findForRead(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 기한_안이면_워터마크_파일을_내려주고_열람을_기록한다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));
        when(ebookFileRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ebookFile()));
        when(storageService.get("originals/original.pdf"))
                .thenReturn("%PDF-original".getBytes(StandardCharsets.UTF_8));
        when(watermarkService.watermark(any(), anyLong(), anyLong(), any()))
                .thenReturn("%PDF-watermarked".getBytes(StandardCharsets.UTF_8));
        when(storageService.putWatermarked(any(), any())).thenReturn("watermarked/grant-1.pdf");

        CampaignEbookService.EbookDownload download = service.download(APPLICATION_ID, "1.2.3.4");

        assertThat(new String(download.content(), StandardCharsets.UTF_8)).isEqualTo("%PDF-watermarked");
        assertThat(grant.getOpenCount()).isEqualTo(1);
        assertThat(grant.getFirstOpenedAt()).isNotNull();
        assertThat(grant.getLastOpenIp()).isEqualTo("1.2.3.4");
        assertThat(grant.getWatermarkStatus()).isEqualTo(WatermarkStatus.READY);
    }

    @Test
    void 기한이_지나면_열람할_수_없다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().minusMinutes(1));
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_ACCESS_EXPIRED);

        assertThat(grant.getOpenCount()).isZero();
        verify(storageService, never()).get(anyString());
    }

    @Test
    void 회수된_권한은_즉시_차단된다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        grant.revoke();
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_ACCESS_REVOKED);

        verify(storageService, never()).get(anyString());
    }

    @Test
    void 선정되지_않아_권한이_없으면_열람할_수_없다() {
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_ACCESS_DENIED);
    }

    @Test
    void 남의_신청은_열람할_수_없다() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(999L, null, List.of()));

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void 중도_포기하면_권한이_회수된다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));

        service.dropOut(APPLICATION_ID);

        assertThat(application.getStatus()).isEqualTo(CampaignApplicationStatus.DROPPED);
        assertThat(grant.isRevoked()).isTrue();
    }

    @Test
    void 저장소_미설정은_워터마크_실패로_뭉개지_않고_그대로_알린다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));
        when(ebookFileRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ebookFile()));
        when(storageService.get(anyString()))
                .thenThrow(new CustomException(ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED));

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED);

        // 파일이 깨진 게 아니므로 권한을 실패 상태로 남기지 않는다.
        assertThat(grant.getWatermarkStatus()).isEqualTo(WatermarkStatus.PENDING);
    }

    @Test
    void 캐시된_워터마크가_있어도_저장소_미설정이면_그대로_알린다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        grant.markWatermarkReady("watermarked/grant-1.pdf");
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));
        when(ebookFileRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ebookFile()));
        when(storageService.get("watermarked/grant-1.pdf"))
                .thenThrow(new CustomException(ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED));

        assertThatThrownBy(() -> service.download(APPLICATION_ID, "1.2.3.4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_STORAGE_NOT_CONFIGURED);

        verify(watermarkService, never()).watermark(any(), anyLong(), anyLong(), any());
    }

    @Test
    void 이미_만든_워터마크_파일은_다시_만들지_않는다() {
        EbookAccessGrant grant = grant(LocalDateTime.now().plusDays(3));
        grant.markWatermarkReady("watermarked/grant-1.pdf");
        when(grantRepository.findForUpdate(APPLICATION_ID)).thenReturn(Optional.of(grant));
        when(ebookFileRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ebookFile()));
        when(storageService.get("watermarked/grant-1.pdf"))
                .thenReturn("%PDF-cached".getBytes(StandardCharsets.UTF_8));

        CampaignEbookService.EbookDownload download = service.download(APPLICATION_ID, "1.2.3.4");

        assertThat(new String(download.content(), StandardCharsets.UTF_8)).isEqualTo("%PDF-cached");
        verify(watermarkService, never()).watermark(any(), anyLong(), anyLong(), any());
    }

    private ReviewCampaign campaign(CampaignDeliveryType deliveryType) {
        OfficialProfile profile = OfficialProfile.builder()
                .type(OfficialProfileType.AUTHOR)
                .displayName("독립작가")
                .slug("indie-author")
                .build();
        Book book = Book.builder()
                .title("테스트 책")
                .author("테스트 작가")
                .source(BookSource.KAKAO)
                .build();
        ReviewCampaign campaign = ReviewCampaign.builder()
                .profile(profile)
                .book(book)
                .title("PDF 서평단")
                .recruitCount(5)
                .recruitStartAt(LocalDateTime.now().minusDays(10))
                .recruitEndAt(LocalDateTime.now().minusDays(5))
                .reviewDueAt(LocalDateTime.now().plusDays(10))
                .deliveryType(deliveryType)
                .ebookAccessExtraDays(7)
                .build();
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private ReviewCampaignApplication selectedApplication() {
        User user = User.create("reader@test.com", "독자", null);
        ReflectionTestUtils.setField(user, "id", READER_ID);

        ReviewCampaignApplication application = ReviewCampaignApplication.builder()
                .campaign(campaign)
                .user(user)
                .message(null)
                .build();
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        application.select();
        return application;
    }

    private EbookAccessGrant grant(LocalDateTime expiresAt) {
        EbookAccessGrant grant = EbookAccessGrant.builder()
                .application(application)
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(grant, "id", 1L);
        return grant;
    }

    private CampaignEbookFile ebookFile() {
        return CampaignEbookFile.builder()
                .campaign(campaign)
                .storageKey("originals/original.pdf")
                .originalFilename("book.pdf")
                .mimeType("application/pdf")
                .byteSize(1024)
                .pageCount(100)
                .build();
    }
}
