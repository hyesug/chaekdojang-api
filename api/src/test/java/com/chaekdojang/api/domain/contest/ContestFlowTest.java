package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.contest.dto.*;
import com.chaekdojang.api.domain.notification.NotificationRepository;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.officialprofile.*;
import com.chaekdojang.api.domain.officialprofile.dto.OfficialProfileCreateRequest;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContestFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired BookRepository books;
    @Autowired ReviewRepository reviews;
    @Autowired OfficialProfileRepository profiles;
    @Autowired OfficialProfileMemberRepository members;
    @Autowired ContestEntryRepository entries;
    @Autowired NotificationRepository notifications;
    @Autowired ContestService contestService;
    @Autowired ContestManageService manageService;
    @Autowired OfficialProfileService officialProfileService;

    Fixture fixture;

    record Fixture(Long librarian, Long admin, Long reader, Long otherReader,
                   Long libraryProfile, Long designatedBook, Long otherBook,
                   Long readerReview, Long readerOtherBookReview, Long otherReaderReview) {}

    @BeforeEach
    void createFixture() {
        fixture = tx.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            User librarian = users.save(User.create("librarian-" + suffix + "@test.com", "도서관담당" + suffix.substring(0, 8), null));
            User admin = users.save(User.create("admin-" + suffix + "@test.com", "운영진" + suffix.substring(0, 8), null));
            admin.setSuperAdmin();
            User reader = users.save(User.create("reader-" + suffix + "@test.com", "독자" + suffix.substring(0, 8), null));
            User otherReader = users.save(User.create("other-" + suffix + "@test.com", "다른독자" + suffix.substring(0, 8), null));

            OfficialProfile library = profiles.save(OfficialProfile.builder()
                    .type(OfficialProfileType.LIBRARY).displayName("시립도서관").slug("library-" + suffix).build());
            members.save(OfficialProfileMember.owner(library, librarian));

            Book designated = books.save(Book.builder().title("지정 도서").author("작가").source(BookSource.KAKAO).build());
            Book other = books.save(Book.builder().title("다른 책").author("작가").source(BookSource.KAKAO).build());

            Review readerReview = reviews.save(Review.builder()
                    .book(designated).author(reader).content("지정 도서로 쓴 독후감").rating(5).build());
            Review readerOtherBookReview = reviews.save(Review.builder()
                    .book(other).author(reader).content("다른 책으로 쓴 독후감").rating(4).build());
            Review otherReaderReview = reviews.save(Review.builder()
                    .book(designated).author(otherReader).content("다른 독자의 독후감").rating(5).build());

            return new Fixture(librarian.getId(), admin.getId(), reader.getId(), otherReader.getId(),
                    library.getId(), designated.getId(), other.getId(),
                    readerReview.getId(), readerOtherBookReview.getId(), otherReaderReview.getId());
        });
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 도서관_공모전을_만들고_응모받아_수상_발표까지_끝낸다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));

        // 작성 중 공모전은 독자에게 보이지 않는다.
        assertThat(contestService.getOpenContests()).noneMatch(contest -> contest.id().equals(contestId));
        authenticate(fixture.reader());
        assertThatThrownBy(() -> contestService.getContest(contestId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_NOT_FOUND);

        authenticate(fixture.librarian());
        updateStatus(contestId, ContestStatus.OPEN);

        authenticate(fixture.reader());
        assertThat(contestService.getSubmittableReviews(contestId))
                .extracting(ContestSubmittableReviewResponse::reviewId)
                .containsExactly(fixture.readerReview());
        MyContestEntryResponse entry = contestService.submit(contestId, submitReview(fixture.readerReview()));
        assertThat(entry.status()).isEqualTo(ContestEntryStatus.SUBMITTED);
        assertThat(contestService.getContest(contestId).myEntry().id()).isEqualTo(entry.id());

        authenticate(fixture.otherReader());
        MyContestEntryResponse otherEntry = contestService.submit(contestId, submitReview(fixture.otherReaderReview()));

        authenticate(fixture.librarian());
        updateStatus(contestId, ContestStatus.CLOSED);
        assertThat(manageService.getEntries(contestId)).hasSize(2);

        manageService.saveAwards(contestId, new ContestAwardSaveRequest(
                List.of(new ContestAwardSaveRequest.AwardItem(entry.id(), 1, "대상"))));
        ManageContestDetailResponse announced = updateStatus(contestId, ContestStatus.ANNOUNCED);
        assertThat(announced.contest().status()).isEqualTo(ContestStatus.ANNOUNCED);
        assertThat(announced.awardedCount()).isEqualTo(1);
        assertThat(announced.notAwardedCount()).isEqualTo(1);

        assertThat(notifications.findAllByReceiverIdOrderByCreatedAtDesc(fixture.reader()))
                .extracting(notification -> notification.getType())
                .contains(NotificationType.CONTEST_AWARDED);
        assertThat(notifications.findAllByReceiverIdOrderByCreatedAtDesc(fixture.otherReader()))
                .extracting(notification -> notification.getType())
                .contains(NotificationType.CONTEST_NOT_AWARDED);

        // 발표 후에는 수상작이 공개된다.
        authenticate(fixture.reader());
        ContestDetailResponse detail = contestService.getContest(contestId);
        assertThat(detail.awards()).extracting(ContestAwardResponse::awardName).containsExactly("대상");
        assertThat(detail.myEntry().awardRank()).isEqualTo(1);
        assertThat(entries.findById(otherEntry.id()).orElseThrow().getStatus())
                .isEqualTo(ContestEntryStatus.NOT_AWARDED);
    }

    @Test
    void 지정_도서로_쓰지_않은_독후감은_응모할_수_없다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));
        updateStatus(contestId, ContestStatus.OPEN);

        authenticate(fixture.reader());
        assertThatThrownBy(() -> contestService.submit(contestId, submitReview(fixture.readerOtherBookReview())))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_ENTRY_BOOK_NOT_ALLOWED);
    }

    @Test
    void 자유주제_전용글_공모전은_제목과_본문으로_응모한다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.TEXT, List.of());
        updateStatus(contestId, ContestStatus.OPEN);

        authenticate(fixture.reader());
        assertThatThrownBy(() -> contestService.submit(contestId,
                new ContestEntrySubmitRequest(null, null, "제목만", null, true)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_ENTRY_TEXT_REQUIRED);

        MyContestEntryResponse entry = contestService.submit(contestId,
                new ContestEntrySubmitRequest(null, null, "내가 만난 책", "본문입니다", true));
        assertThat(entry.entryTitle()).isEqualTo("내가 만난 책");
        assertThat(entry.bookId()).isNull();
    }

    @Test
    void 응모를_취소하면_같은_공모전에_다시_응모할_수_있다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));
        updateStatus(contestId, ContestStatus.OPEN);

        authenticate(fixture.reader());
        MyContestEntryResponse entry = contestService.submit(contestId, submitReview(fixture.readerReview()));
        assertThatThrownBy(() -> contestService.submit(contestId, submitReview(fixture.readerReview())))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_ALREADY_ENTERED);

        assertThat(contestService.withdraw(entry.id()).status()).isEqualTo(ContestEntryStatus.WITHDRAWN);
        MyContestEntryResponse resubmitted = contestService.submit(contestId, submitReview(fixture.readerReview()));
        assertThat(resubmitted.id()).isEqualTo(entry.id());
        assertThat(resubmitted.status()).isEqualTo(ContestEntryStatus.SUBMITTED);
    }

    @Test
    void 수상작을_지정하지_않으면_발표할_수_없다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));
        updateStatus(contestId, ContestStatus.OPEN);
        updateStatus(contestId, ContestStatus.CLOSED);

        assertThatThrownBy(() -> updateStatus(contestId, ContestStatus.ANNOUNCED))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_AWARD_REQUIRED);
    }

    @Test
    void 심사_단계가_아니면_수상자를_지정할_수_없다() {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));
        updateStatus(contestId, ContestStatus.OPEN);

        authenticate(fixture.reader());
        MyContestEntryResponse entry = contestService.submit(contestId, submitReview(fixture.readerReview()));

        authenticate(fixture.librarian());
        assertThatThrownBy(() -> manageService.saveAwards(contestId, new ContestAwardSaveRequest(
                List.of(new ContestAwardSaveRequest.AwardItem(entry.id(), 1, "대상")))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_AWARD_STAGE_INVALID);
    }

    @Test
    void 책도장_주최_프로필은_관리자만_만들고_공모전을_열_수_있다() {
        authenticateAdmin(fixture.admin());
        Long platformProfileId = officialProfileService.createProfile(fixture.admin(),
                new OfficialProfileCreateRequest(OfficialProfileType.PLATFORM, "책도장", null, null, null)).id();

        assertThat(manageService.getHostProfiles())
                .anyMatch(profile -> profile.id().equals(platformProfileId) && profile.platform());
        Long contestId = createContest(platformProfileId, ContestEntryType.REVIEW, List.of());
        assertThat(manageService.getContestDetail(contestId).contest().platformHosted()).isTrue();

        // 소속되지 않은 일반 사용자는 이 프로필로 공모전을 열 수 없다.
        authenticate(fixture.reader());
        assertThatThrownBy(() -> createContest(platformProfileId, ContestEntryType.REVIEW, List.of()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        assertThat(manageService.getHostProfiles()).isEmpty();
    }

    @Test
    void 공모전_목록과_상세는_비회원도_보고_응모와_운영은_로그인을_요구한다() throws Exception {
        authenticate(fixture.librarian());
        Long contestId = createContest(fixture.libraryProfile(), ContestEntryType.REVIEW, List.of(fixture.designatedBook()));
        updateStatus(contestId, ContestStatus.OPEN);

        // MockMvc는 테스트 스레드의 인증을 물려받으므로 비회원 요청임을 명시한다.
        mvc.perform(get("/api/contests").with(anonymous())).andExpect(status().isOk());
        mvc.perform(get("/api/contests/" + contestId).with(anonymous())).andExpect(status().isOk());
        mvc.perform(get("/api/contests/me/entries").with(anonymous()))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/contests/" + contestId + "/submittable-reviews").with(anonymous()))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/contests/manage/contests").with(anonymous()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 접수_시작보다_이른_마감이나_마감보다_이른_발표는_거부한다() {
        authenticate(fixture.librarian());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        assertThatThrownBy(() -> manageService.createContest(fixture.libraryProfile(), new ContestCreateRequest(
                "잘못된 기간", null, null, ContestEntryType.REVIEW, List.of(),
                now.plusDays(5), now.plusDays(1), now.plusDays(10))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_INVALID_PERIOD);

        assertThatThrownBy(() -> manageService.createContest(fixture.libraryProfile(), new ContestCreateRequest(
                "잘못된 발표일", null, null, ContestEntryType.REVIEW, List.of(),
                now.minusDays(1), now.plusDays(5), now.plusDays(2))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTEST_INVALID_PERIOD);
    }

    private Long createContest(Long profileId, ContestEntryType entryType, List<Long> bookIds) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return manageService.createContest(profileId, new ContestCreateRequest(
                "독후감 공모전", "요강입니다", "대상 1명 도서상품권", entryType, bookIds,
                now.minusMinutes(1), now.plusDays(7), now.plusDays(14))).contest().id();
    }

    private ManageContestDetailResponse updateStatus(Long contestId, ContestStatus status) {
        return manageService.updateStatus(contestId, new ContestStatusUpdateRequest(status));
    }

    private ContestEntrySubmitRequest submitReview(Long reviewId) {
        return new ContestEntrySubmitRequest(reviewId, null, null, null, true);
    }

    private static void authenticate(Long id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, List.of()));
    }

    private static void authenticateAdmin(Long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
    }
}
