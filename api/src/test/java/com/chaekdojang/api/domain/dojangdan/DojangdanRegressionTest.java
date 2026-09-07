package com.chaekdojang.api.domain.dojangdan;

import com.chaekdojang.api.domain.book.*;
import com.chaekdojang.api.domain.dojangdan.dto.CampaignUpdateRequest;
import com.chaekdojang.api.domain.notification.*;
import com.chaekdojang.api.domain.officialprofile.*;
import com.chaekdojang.api.domain.user.*;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties = "app.dojangdan.invite-scheduler-delay-ms=3600000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DojangdanRegressionTest {
    static final Path storageRoot;
    static {
        try { storageRoot = Files.createTempDirectory("dojangdan-regression-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.type", () -> "local");
        registry.add("app.storage.local.ebook-dir", storageRoot::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired BookRepository books;
    @Autowired OfficialProfileRepository profiles;
    @Autowired OfficialProfileMemberRepository members;
    @Autowired ReviewCampaignRepository campaigns;
    @Autowired ReviewCampaignApplicationRepository applications;
    @Autowired EbookAccessGrantRepository grants;
    @Autowired EbookOpenLogRepository logs;
    @Autowired ProfileFollowIntentRepository intents;
    @Autowired ProfileInviteSendRepository sends;
    @Autowired NotificationRepository notifications;
    @Autowired CampaignEbookService ebooks;
    @Autowired DojangdanManageService manage;
    @Autowired CampaignInviteService invites;
    @Autowired NotificationService notificationService;

    Fixture fixture;
    record Fixture(Long owner, Long reader, Long profile, Long book, Long campaign, Long application, Long grant) {}

    @BeforeEach
    void createFixture() {
        fixture = tx.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            User owner = users.save(User.create("owner-" + suffix + "@test.com", "주최" + suffix.substring(0, 8), null));
            User reader = users.save(User.create("reader-" + suffix + "@test.com", "독자" + suffix.substring(0, 8), null));
            OfficialProfile profile = profiles.save(OfficialProfile.builder().type(OfficialProfileType.AUTHOR)
                    .displayName("작가").slug(suffix).build());
            members.save(OfficialProfileMember.owner(profile, owner));
            Book book = books.save(Book.builder().title("테스트 책").author("작가").source(BookSource.KAKAO).build());
            ReviewCampaign campaign = campaigns.save(newCampaign(profile, book));
            ReviewCampaignApplication application = applications.save(ReviewCampaignApplication.builder()
                    .campaign(campaign).user(reader).build());
            application.select();
            EbookAccessGrant grant = grants.save(EbookAccessGrant.builder().application(application)
                    .expiresAt(campaign.ebookExpiresAt()).build());
            return new Fixture(owner.getId(), reader.getId(), profile.getId(), book.getId(),
                    campaign.getId(), application.getId(), grant.getId());
        });
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @AfterAll
    static void removeTestFiles() throws Exception {
        try (var paths = Files.walk(storageRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @Test
    void 기존_공개_경로는_비회원과_회원_모두_전자책을_읽지_못한다() throws Exception {
        Path legacy = Path.of("uploads/campaign-ebooks/review-" + UUID.randomUUID() + ".pdf");
        Files.createDirectories(legacy.getParent());
        try {
            Files.writeString(legacy, "private-book-sentinel");
            String url = "/" + legacy.toString().replace('\\', '/');
            mvc.perform(get(url)).andExpect(status().is4xxClientError());
            mvc.perform(get(url).with(user("reader"))).andExpect(status().isForbidden());
            mvc.perform(get("/uploads/other-private-folder/book.pdf")).andExpect(status().isNotFound());
        } finally { Files.deleteIfExists(legacy); }
    }

    @Test
    void PDF_교체_후_새_판본을_받고_IP별_열람_기록이_남는다() throws Exception {
        authenticate(fixture.owner());
        ebooks.upload(fixture.campaign(), pdf(1));
        authenticate(fixture.reader());
        assertPages(ebooks.download(fixture.application(), "192.0.2.1").content(), 1);
        authenticate(fixture.owner());
        ebooks.upload(fixture.campaign(), pdf(2));
        authenticate(fixture.reader());
        assertPages(ebooks.download(fixture.application(), "192.0.2.2").content(), 2);
        tx.executeWithoutResult(status -> {
            var history = logs.findAll().stream().filter(log -> log.getGrant().getId().equals(fixture.grant())).toList();
            assertThat(history).extracting(EbookOpenLog::getIp).containsExactlyInAnyOrder("192.0.2.1", "192.0.2.2");
            assertThat(history).extracting(EbookOpenLog::getSourceStorageKey).doesNotHaveDuplicates();
            assertThat(grants.findById(fixture.grant()).orElseThrow().getOpenCount()).isEqualTo(2);
        });
    }

    @Test
    void 마감_변경은_기존_권한에_반영되고_철회는_되살리지_않는다() {
        authenticate(fixture.owner());
        LocalDateTime newDue = LocalDateTime.now().plusDays(30).withNano(0);
        updateDue(newDue);
        assertThat(grants.findById(fixture.grant()).orElseThrow().getExpiresAt()).isEqualTo(newDue.plusDays(7));
        authenticate(fixture.reader());
        ebooks.dropOut(fixture.application());
        authenticate(fixture.owner());
        updateDue(newDue.plusDays(10));
        assertThat(grants.findById(fixture.grant()).orElseThrow().isRevoked()).isTrue();
        authenticate(fixture.reader());
        assertThatThrownBy(() -> ebooks.download(fixture.application(), "192.0.2.1"))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_ACCESS_REVOKED);
    }

    @Test
    void 기한을_단축하면_기존_선정자도_즉시_만료된다() {
        authenticate(fixture.owner());
        LocalDateTime now = LocalDateTime.now();
        manage.updateCampaign(fixture.campaign(), new CampaignUpdateRequest("마감된 모집", null, 5,
                now.minusDays(5), now.minusDays(3), now.minusDays(1), null, null, 0));
        authenticate(fixture.reader());
        assertThatThrownBy(() -> ebooks.download(fixture.application(), "192.0.2.1"))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.EBOOK_ACCESS_EXPIRED);
    }

    @Test
    void 동시에_다운로드해도_횟수와_IP_기록이_유실되지_않는다() throws Exception {
        authenticate(fixture.owner());
        ebooks.upload(fixture.campaign(), pdf(1));
        runTogether(() -> downloadAsReader("192.0.2.3"), () -> downloadAsReader("192.0.2.4"));
        assertThat(grants.findById(fixture.grant()).orElseThrow().getOpenCount()).isEqualTo(2);
        tx.executeWithoutResult(status -> assertThat(logs.findAll().stream()
                .filter(log -> log.getGrant().getId().equals(fixture.grant()))).hasSize(2));
    }

    @Test
    void 서로_다른_캠페인을_동시에_열어도_30일_2회를_넘지_않는다() throws Exception {
        List<Long> ids = tx.execute(status -> {
            var profile = profiles.findById(fixture.profile()).orElseThrow();
            var book = books.findById(fixture.book()).orElseThrow();
            var reader = users.findById(fixture.reader()).orElseThrow();
            intents.save(ProfileFollowIntent.builder().user(reader).profile(profile).build());
            return List.of(campaigns.save(newCampaign(profile, book)).getId(),
                    campaigns.save(newCampaign(profile, book)).getId());
        });
        send(fixture.campaign());
        runTogether(() -> send(ids.get(0)), () -> send(ids.get(1)));
        assertThat(sends.countByProfileIdAndUserIdAndSentAtAfter(fixture.profile(), fixture.reader(),
                LocalDateTime.now().minusDays(30))).isEqualTo(2);
        assertThat(notifications.findAllByReceiverIdOrderByCreatedAtDesc(fixture.reader())).hasSize(2);
    }

    @Test
    void 예약_초대는_시작일에_한번만_발송되고_알림에서_수신거부할_수_있다() {
        tx.executeWithoutResult(status -> {
            var campaign = campaigns.findById(fixture.campaign()).orElseThrow();
            LocalDateTime start = LocalDateTime.now().plusDays(3);
            campaign.update(campaign.getTitle(), null, 5, start, start.plusDays(7), start.plusDays(14), 24, null, null);
            campaign.setPriorityInviteSender(users.findById(fixture.owner()).orElseThrow());
            campaign.startRecruiting();
            intents.save(ProfileFollowIntent.builder().user(users.findById(fixture.reader()).orElseThrow())
                    .profile(campaign.getProfile()).build());
        });
        invites.sendDueInvites(fixture.campaign());
        assertThat(sends.countByCampaignId(fixture.campaign())).isZero();
        tx.executeWithoutResult(status -> {
            var campaign = campaigns.findById(fixture.campaign()).orElseThrow();
            LocalDateTime start = LocalDateTime.now().minusMinutes(1);
            campaign.update(campaign.getTitle(), null, 5, start, start.plusDays(7), start.plusDays(14), 24, null, null);
        });
        invites.sendDueInvites(fixture.campaign());
        invites.sendDueInvites(fixture.campaign());
        assertThat(sends.countByCampaignId(fixture.campaign())).isEqualTo(1);
        Long notificationId = notifications.findAllByReceiverIdOrderByCreatedAtDesc(fixture.reader()).get(0).getId();
        authenticate(fixture.owner());
        assertThatThrownBy(() -> notificationService.unsubscribeCampaignInvitation(notificationId))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        authenticate(fixture.reader());
        notificationService.unsubscribeCampaignInvitation(notificationId);
        notificationService.unsubscribeCampaignInvitation(notificationId);
        assertThat(intents.existsByUserIdAndProfileIdAndUnsubscribedAtIsNull(fixture.reader(), fixture.profile())).isFalse();
    }

    private void updateDue(LocalDateTime due) {
        var campaign = campaigns.findById(fixture.campaign()).orElseThrow();
        manage.updateCampaign(fixture.campaign(), new CampaignUpdateRequest(campaign.getTitle(), null, 5,
                campaign.getRecruitStartAt(), campaign.getRecruitEndAt(), due, null, null, 7));
    }

    private ReviewCampaign newCampaign(OfficialProfile profile, Book book) {
        return ReviewCampaign.builder().profile(profile).book(book).title("회귀 검증").recruitCount(5)
                .recruitStartAt(LocalDateTime.now().minusDays(2)).recruitEndAt(LocalDateTime.now().plusDays(5))
                .reviewDueAt(LocalDateTime.now().plusDays(10)).deliveryType(CampaignDeliveryType.PDF).build();
    }

    private void send(Long campaignId) {
        tx.executeWithoutResult(status -> invites.sendPriorityInvites(campaigns.findById(campaignId).orElseThrow(),
                users.findById(fixture.owner()).orElseThrow()));
    }

    private void downloadAsReader(String ip) {
        authenticate(fixture.reader());
        try { ebooks.download(fixture.application(), ip); }
        finally { SecurityContextHolder.clearContext(); }
    }

    private static void authenticate(Long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
    }

    private static MockMultipartFile pdf(int pages) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) document.addPage(new PDPage());
            document.save(out);
            return new MockMultipartFile("file", "book.pdf", "application/pdf", out.toByteArray());
        }
    }

    private static void assertPages(byte[] bytes, int pages) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) { assertThat(document.getNumberOfPages()).isEqualTo(pages); }
    }

    private static void runTogether(Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Runnable action : List.of(first, second)) futures.add(pool.submit(() -> {
                try { start.await(); action.run(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            start.countDown();
            for (Future<?> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
}
