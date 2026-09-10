package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.contest.dto.*;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 독자용 공모전 기능.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ContestRepository contestRepository;
    private final ContestBookRepository contestBookRepository;
    private final ContestEntryRepository entryRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /** 공개 공모전 목록. 작성 중(DRAFT)은 제외한다. */
    public List<ContestSummaryResponse> getOpenContests() {
        return contestRepository.findByStatusInOrderBySubmitEndAtDesc(List.of(
                        ContestStatus.OPEN, ContestStatus.CLOSED, ContestStatus.ANNOUNCED))
                .stream()
                .map(contest -> ContestSummaryResponse.of(contest, entryCount(contest.getId()), books(contest.getId())))
                .toList();
    }

    public ContestDetailResponse getContest(Long contestId) {
        Contest contest = findPublicContest(contestId);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();

        MyContestEntryResponse myEntry = userId == null ? null
                : entryRepository.findByContestIdAndUserId(contestId, userId)
                .map(MyContestEntryResponse::from)
                .orElse(null);

        List<ContestAwardResponse> awards = contest.getStatus() == ContestStatus.ANNOUNCED
                ? entryRepository.findByContestIdAndStatusOrderByAwardRankAsc(contestId, ContestEntryStatus.AWARDED)
                .stream().map(ContestAwardResponse::from).toList()
                : List.of();

        return ContestDetailResponse.of(
                contest,
                entryCount(contestId),
                books(contestId),
                contest.isAcceptingEntries(LocalDateTime.now(KST)),
                myEntry,
                awards
        );
    }

    /** 이 공모전에 낼 수 있는 내 독후감 후보. 지정 도서가 있으면 그 책으로 쓴 독후감만 보여준다. */
    public List<ContestSubmittableReviewResponse> getSubmittableReviews(Long contestId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Contest contest = findPublicContest(contestId);
        if (contest.getEntryType() != ContestEntryType.REVIEW) return List.of();

        List<Long> bookIds = designatedBookIds(contestId);
        List<Review> reviews = bookIds.isEmpty()
                ? reviewRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                : bookIds.stream()
                .flatMap(bookId -> reviewRepository
                        .findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, bookId)
                        .stream())
                .toList();

        return reviews.stream().map(ContestSubmittableReviewResponse::from).toList();
    }

    @Transactional
    public MyContestEntryResponse submit(Long contestId, ContestEntrySubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Contest contest = findPublicContest(contestId);

        if (!request.agreeTerms()) {
            throw new CustomException(ErrorCode.CONTEST_TERMS_REQUIRED);
        }
        if (!contest.isAcceptingEntries(LocalDateTime.now(KST))) {
            throw new CustomException(ErrorCode.CONTEST_NOT_OPEN);
        }

        List<ContestBook> designated = contestBookRepository.findByContestIdOrderByIdAsc(contestId);
        Review review = null;
        Book book;
        String title = null;
        String content = null;

        if (contest.getEntryType() == ContestEntryType.REVIEW) {
            if (request.reviewId() == null) {
                throw new CustomException(ErrorCode.CONTEST_ENTRY_REVIEW_REQUIRED);
            }
            review = reviewRepository.findById(request.reviewId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
            if (review.getDeletedAt() != null || !review.isAuthor(userId)) {
                throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
            }
            book = review.getBook();
            if (!designated.isEmpty() && !isDesignated(designated, book)) {
                throw new CustomException(ErrorCode.CONTEST_ENTRY_BOOK_NOT_ALLOWED);
            }
        } else {
            if (isBlank(request.title()) || isBlank(request.content())) {
                throw new CustomException(ErrorCode.CONTEST_ENTRY_TEXT_REQUIRED);
            }
            title = request.title().trim();
            content = request.content().trim();
            book = resolveDesignatedBook(designated, request.bookId());
        }

        var existing = entryRepository.findByContestIdAndUserId(contestId, userId);
        if (existing.isPresent()) {
            ContestEntry entry = existing.get();
            // 취소한 응모는 같은 행을 다시 채운다. 사람당 한 행만 두기 때문이다.
            if (entry.getStatus() != ContestEntryStatus.WITHDRAWN) {
                throw new CustomException(ErrorCode.CONTEST_ALREADY_ENTERED);
            }
            entry.resubmit(review, book, title, content);
            return MyContestEntryResponse.from(entry);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ContestEntry entry = entryRepository.save(ContestEntry.builder()
                .contest(contest)
                .user(user)
                .review(review)
                .book(book)
                .title(title)
                .content(content)
                .build());
        return MyContestEntryResponse.from(entry);
    }

    public List<MyContestEntryResponse> getMyEntries() {
        Long userId = SecurityUtils.getCurrentUserId();
        return entryRepository.findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(MyContestEntryResponse::from)
                .toList();
    }

    /** 접수 기간 안에서만 응모를 취소할 수 있다. */
    @Transactional
    public MyContestEntryResponse withdraw(Long entryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ContestEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEST_ENTRY_NOT_FOUND));
        if (!entry.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (entry.getStatus() != ContestEntryStatus.SUBMITTED
                || !entry.getContest().isAcceptingEntries(LocalDateTime.now(KST))) {
            throw new CustomException(ErrorCode.CONTEST_ENTRY_WITHDRAW_NOT_ALLOWED);
        }
        entry.withdraw();
        return MyContestEntryResponse.from(entry);
    }

    private Contest findPublicContest(Long contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEST_NOT_FOUND));
        if (!contest.isPublic()) {
            throw new CustomException(ErrorCode.CONTEST_NOT_FOUND);
        }
        return contest;
    }

    private List<ContestBookResponse> books(Long contestId) {
        return contestBookRepository.findByContestIdOrderByIdAsc(contestId).stream()
                .map(contestBook -> ContestBookResponse.from(contestBook.getBook()))
                .toList();
    }

    private List<Long> designatedBookIds(Long contestId) {
        return contestBookRepository.findByContestIdOrderByIdAsc(contestId).stream()
                .map(contestBook -> contestBook.getBook().getId())
                .toList();
    }

    private long entryCount(Long contestId) {
        return entryRepository.countByContestIdAndStatusNot(contestId, ContestEntryStatus.WITHDRAWN);
    }

    private boolean isDesignated(List<ContestBook> designated, Book book) {
        return book != null && designated.stream()
                .anyMatch(contestBook -> contestBook.getBook().getId().equals(book.getId()));
    }

    /** 지정 도서가 있는 공모전이면 어느 책으로 냈는지 반드시 골라야 한다. */
    private Book resolveDesignatedBook(List<ContestBook> designated, Long bookId) {
        if (designated.isEmpty()) return null;
        if (bookId == null) {
            throw new CustomException(ErrorCode.CONTEST_ENTRY_BOOK_REQUIRED);
        }
        Map<Long, Book> byId = designated.stream()
                .map(ContestBook::getBook)
                .collect(Collectors.toMap(Book::getId, Function.identity(), (first, second) -> first));
        Book book = byId.get(bookId);
        if (book == null) {
            throw new CustomException(ErrorCode.CONTEST_ENTRY_BOOK_NOT_ALLOWED);
        }
        return book;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
