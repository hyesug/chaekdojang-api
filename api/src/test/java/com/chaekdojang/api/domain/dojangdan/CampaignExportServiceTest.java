package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.admin.audit.AdminAuditLogService;
import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.dojangdan.dto.CampaignReviewSummaryResponse;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignExportServiceTest {

    private static final Long CAMPAIGN_ID = 100L;
    private static final Long PUBLISHER_USER_ID = 1L;

    @Mock ReviewCampaignApplicationRepository applicationRepository;
    @Mock ReviewUsageConsentRepository consentRepository;
    @Mock UserRepository userRepository;
    @Mock CampaignAccessGuard accessGuard;
    @Mock AdminAuditLogService auditLogService;
    @InjectMocks CampaignExportService service;

    private ReviewCampaign campaign;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://www.chaekdojang.com");
        campaign = campaign();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PUBLISHER_USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 동의한_독후감만_내보낸다() {
        ReviewCampaignApplication agreed = submittedApplication(1L, "동의한독자", "동의한 독후감 본문");
        ReviewCampaignApplication refused = submittedApplication(2L, "거부한독자", "거부한 독후감 본문");

        when(accessGuard.requireCampaignAccess(CAMPAIGN_ID)).thenReturn(campaign);
        when(applicationRepository.findByCampaignIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(agreed, refused));
        when(consentRepository.findByApplicationIdInAndRevokedAtIsNull(any()))
                .thenReturn(List.of(
                        consent(agreed, true, ConsentDisplayNameType.REAL_NICKNAME, false),
                        consent(refused, false, ConsentDisplayNameType.REAL_NICKNAME, false)));
        when(userRepository.findById(PUBLISHER_USER_ID)).thenReturn(Optional.empty());

        String markdown = new String(service.export(CAMPAIGN_ID, "markdown").content(), StandardCharsets.UTF_8);

        assertThat(markdown).contains("동의한독자", "동의한 독후감 본문");
        assertThat(markdown).doesNotContain("거부한독자", "거부한 독후감 본문");
        assertThat(markdown).contains("활용 동의 독후감: 1건");
    }

    @Test
    void 철회한_동의는_내보내기에서_빠진다() {
        ReviewCampaignApplication revoked = submittedApplication(1L, "철회한독자", "철회한 독후감 본문");

        when(accessGuard.requireCampaignAccess(CAMPAIGN_ID)).thenReturn(campaign);
        when(applicationRepository.findByCampaignIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(revoked));
        // 철회된 동의는 findByApplicationIdInAndRevokedAtIsNull 결과에 포함되지 않는다.
        when(consentRepository.findByApplicationIdInAndRevokedAtIsNull(any())).thenReturn(List.of());
        when(userRepository.findById(PUBLISHER_USER_ID)).thenReturn(Optional.empty());

        String markdown = new String(service.export(CAMPAIGN_ID, "markdown").content(), StandardCharsets.UTF_8);

        assertThat(markdown).doesNotContain("철회한독자", "철회한 독후감 본문");
        assertThat(markdown).contains("활용 동의 독후감: 0건");
    }

    @Test
    void 익명_표기를_고르면_닉네임_대신_익명으로_나간다() {
        ReviewCampaignApplication anonymous = submittedApplication(1L, "실제닉네임", "익명 독후감 본문");

        when(accessGuard.requireCampaignAccess(CAMPAIGN_ID)).thenReturn(campaign);
        when(applicationRepository.findByCampaignIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(anonymous));
        when(consentRepository.findByApplicationIdInAndRevokedAtIsNull(any()))
                .thenReturn(List.of(consent(anonymous, true, ConsentDisplayNameType.ANONYMOUS, true)));
        when(userRepository.findById(PUBLISHER_USER_ID)).thenReturn(Optional.empty());

        String markdown = new String(service.export(CAMPAIGN_ID, "markdown").content(), StandardCharsets.UTF_8);

        assertThat(markdown).contains("## 익명", "익명 독후감 본문", "발췌·편집: 허용");
        assertThat(markdown).doesNotContain("실제닉네임");
    }

    @Test
    void 삭제된_독후감은_내보내기에서_빠진다() {
        ReviewCampaignApplication deleted = submittedApplication(1L, "삭제한독자", "삭제된 독후감 본문");
        deleted.getReview().softDelete();

        when(accessGuard.requireCampaignAccess(CAMPAIGN_ID)).thenReturn(campaign);
        when(applicationRepository.findByCampaignIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(deleted));
        when(userRepository.findById(PUBLISHER_USER_ID)).thenReturn(Optional.empty());

        String markdown = new String(service.export(CAMPAIGN_ID, "markdown").content(), StandardCharsets.UTF_8);

        assertThat(markdown).doesNotContain("삭제한독자", "삭제된 독후감 본문");
    }

    @Test
    void 미동의_독후감은_목록에서_본문_없이_보인다() {
        ReviewCampaignApplication refused = submittedApplication(1L, "거부한독자", "거부한 독후감 본문");

        when(accessGuard.requireCampaignAccess(CAMPAIGN_ID)).thenReturn(campaign);
        when(applicationRepository.findByCampaignIdAndStatusIn(anyLong(), any()))
                .thenReturn(List.of(refused));
        when(consentRepository.findByApplicationIdInAndRevokedAtIsNull(any()))
                .thenReturn(List.of(consent(refused, false, ConsentDisplayNameType.REAL_NICKNAME, false)));

        List<CampaignReviewSummaryResponse> reviews = service.getCampaignReviews(CAMPAIGN_ID);

        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).consentPromotional()).isFalse();
        assertThat(reviews.get(0).content()).isNull();
        assertThat(reviews.get(0).reviewUrl()).isEqualTo("https://www.chaekdojang.com/reviews/1");
        assertThat(reviews.get(0).reviewLength()).isEqualTo("거부한 독후감 본문".length());
    }

    private ReviewCampaign campaign() {
        OfficialProfile profile = OfficialProfile.builder()
                .type(OfficialProfileType.PUBLISHER)
                .displayName("테스트출판사")
                .slug("test-publisher")
                .build();
        Book book = Book.builder()
                .title("테스트 책")
                .author("테스트 작가")
                .source(BookSource.KAKAO)
                .build();
        ReviewCampaign campaign = ReviewCampaign.builder()
                .profile(profile)
                .book(book)
                .title("테스트 서평단")
                .recruitCount(5)
                .recruitStartAt(LocalDateTime.now().minusDays(10))
                .recruitEndAt(LocalDateTime.now().minusDays(5))
                .reviewDueAt(LocalDateTime.now().plusDays(5))
                .build();
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private ReviewCampaignApplication submittedApplication(Long id, String nickname, String content) {
        User user = User.create(nickname + "@test.com", nickname, null);
        ReflectionTestUtils.setField(user, "id", id);

        Review review = Review.builder()
                .book(campaign.getBook())
                .author(user)
                .content(content)
                .rating(5)
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.now().minusDays(1));

        ReviewCampaignApplication application = ReviewCampaignApplication.builder()
                .campaign(campaign)
                .user(user)
                .message(null)
                .build();
        ReflectionTestUtils.setField(application, "id", id);
        application.select();
        application.submit(review);
        return application;
    }

    private ReviewUsageConsent consent(ReviewCampaignApplication application, boolean promotional,
                                       ConsentDisplayNameType displayNameType, boolean excerpt) {
        return ReviewUsageConsent.builder()
                .application(application)
                .consentPromotional(promotional)
                .consentExcerpt(excerpt)
                .displayNameType(displayNameType)
                .termsVersion("2026-09-01")
                .consentIp("127.0.0.1")
                .build();
    }
}
