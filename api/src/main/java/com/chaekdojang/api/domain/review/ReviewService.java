package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.library.Library;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.library.LibraryStatus;
import com.chaekdojang.api.domain.review.dto.ReviewCreateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.review.dto.ReviewUpdateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewVisibilityRequest;
import com.chaekdojang.api.domain.user.FollowRepository;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private static final String EDITION_KEYWORDS = "초판|개정|양장|표지|오리지널|리커버|특별|한정|무선|반양장";
    private static final String EDITION_NOTE_PATTERN = "\\((?=[^)]*(" + EDITION_KEYWORDS + "))[^)]*\\)|\\[(?=[^]]*(" + EDITION_KEYWORDS + "))[^]]*\\]";
    private static final String EDITION_COLON_NOTE_PATTERN = "[:：]\\s*(?=.*(" + EDITION_KEYWORDS + ")).*$";
    private static final String NOTE_PATTERN = "\\(([^)]*)\\)|\\[([^]]*)\\]";
    private static final String ENGLISH_SUBTITLE_PATTERN = "\\s+/\\s+[A-Za-z][A-Za-z\\s.'-]*$";
    private static final String EDITION_SUFFIX_PATTERN = "\\s+(더클래식\\s*)?세계문학.*$";

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final LibraryRepository libraryRepository;

    @Transactional
    public ReviewResponse create(ReviewCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Book book = null;
        if (request.bookId() != null) {
            book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        }
        Review review = Review.builder()
                .author(author).book(book)
                .content(request.content()).rating(request.rating())
                .build();
        ReviewResponse saved = ReviewResponse.from(reviewRepository.save(review), 0L, 0L);

        // 책이 있으면 서재에 완독으로 자동 등록 (이미 있으면 상태만 업데이트)
        final Book finalBook = book;
        if (finalBook != null) {
            libraryRepository.findByUserIdAndBookId(userId, finalBook.getId())
                    .ifPresentOrElse(
                            lib -> lib.updateStatus(LibraryStatus.FINISHED, null),
                            () -> libraryRepository.save(
                                    Library.builder().user(author).book(finalBook).status(LibraryStatus.FINISHED).build()
                            )
                    );
        }

        return saved;
    }

    // 페이지네이션 기본: page=0, size=10 / sort: recent(최신순) | rating(별점순) | popular(인기순)
    public Page<ReviewResponse> getAll(int page, int size, String sort) {
        if ("popular".equals(sort)) {
            LocalDateTime now = LocalDateTime.now();
            return toResponsePage(reviewRepository.findAllByPopularity(
                    now.minusDays(7),
                    now.minusDays(30),
                    PageRequest.of(page, size)));
        }
        Sort order = "rating".equals(sort)
                ? Sort.by("rating").descending().and(Sort.by("createdAt").descending())
                : Sort.by("createdAt").descending();
        Page<Review> reviewPage = reviewRepository.findAllByDeletedAtIsNullAndHiddenFalse(
                PageRequest.of(page, size, order));
        return toResponsePage(reviewPage);
    }

    @Transactional
    public void recordView(Long id) {
        Review review = findVisibleReview(id);
        review.increaseViewCount();
    }

    public ReviewResponse getOne(Long id) {
        Review review = findActiveReview(id);
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (review.isHidden()
                && !review.isAuthor(currentUserId)
                && !SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return ReviewResponse.from(review,
                reviewLikeRepository.countByReviewId(id),
                commentRepository.countByReviewIdAndDeletedAtIsNull(id));
    }

    @Transactional
    public ReviewResponse update(Long id, ReviewUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = findActiveReview(id);
        if (!review.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        review.update(request.content(), request.rating());
        return ReviewResponse.from(review,
                reviewLikeRepository.countByReviewId(id),
                commentRepository.countByReviewIdAndDeletedAtIsNull(id));
    }

    @Transactional
    public ReviewResponse updateVisibility(Long id, ReviewVisibilityRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = findActiveReview(id);
        if (!review.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        if (Boolean.TRUE.equals(request.hidden())) review.hide(); else review.unhide();
        return ReviewResponse.from(review,
                reviewLikeRepository.countByReviewId(id),
                commentRepository.countByReviewIdAndDeletedAtIsNull(id));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = findActiveReview(id);
        if (!review.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        review.softDelete();
    }

    public List<ReviewResponse> getFeed() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(userId);
        if (followingIds.isEmpty()) return List.of();
        return toResponseList(
                reviewRepository.findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(followingIds));
    }

    public Page<ReviewResponse> getMyReviews(int page, int size, String q) {
        Long myId = SecurityUtils.getCurrentUserId();
        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;
        return toResponsePage(
                reviewRepository.findByAuthorWithSearch(myId, keyword, PageRequest.of(page, size)));
    }

    public List<ReviewResponse> getByTaste(List<Long> recommendedIds) {
        if (recommendedIds.isEmpty()) return List.of();
        return toResponseList(
                reviewRepository.findAllByAuthorIdInAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(recommendedIds));
    }

    public List<ReviewResponse> getByUser(Long userId) {
        return toResponseList(
                reviewRepository.findAllByAuthorIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(userId));
    }

    public List<ReviewResponse> getByBook(Long bookId) {
        return getByBook(bookId, "recent");
    }

    public List<ReviewResponse> getByBook(Long bookId, String sort) {
        List<Review> reviews;
        if ("popular".equals(sort)) {
            LocalDateTime now = LocalDateTime.now();
            reviews = reviewRepository.findAllByBookIdOrderByPopularity(
                    bookId,
                    now.minusDays(7),
                    now.minusDays(30));
        } else if ("rating".equals(sort)) {
            reviews = reviewRepository.findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByRatingDescCreatedAtDesc(bookId);
        } else {
            reviews = reviewRepository.findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(bookId);
        }
        return toResponseList(reviews);
    }

    public List<ReviewResponse> getByBookWork(String title, String author) {
        String normalizedTitle = normalizeWorkTitle(title);
        String normalizedAuthor = normalizeAuthor(author);
        if (normalizedTitle.isBlank() || normalizedAuthor.isBlank()) return List.of();
        return toResponseList(
                reviewRepository.findAllByBookTitleAndAuthorLike(normalizedTitle, normalizedAuthor));
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    private Page<ReviewResponse> toResponsePage(Page<Review> page) {
        List<Long> ids = page.stream().map(Review::getId).toList();
        Map<Long, Long> likeMap = buildLikeCountMap(ids);
        Map<Long, Long> commentMap = buildCommentCountMap(ids);
        return page.map(r -> ReviewResponse.from(r,
                likeMap.getOrDefault(r.getId(), 0L),
                commentMap.getOrDefault(r.getId(), 0L)));
    }

    private List<ReviewResponse> toResponseList(List<Review> reviews) {
        if (reviews.isEmpty()) return List.of();
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, Long> likeMap = buildLikeCountMap(ids);
        Map<Long, Long> commentMap = buildCommentCountMap(ids);
        return reviews.stream()
                .map(r -> ReviewResponse.from(r,
                        likeMap.getOrDefault(r.getId(), 0L),
                        commentMap.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    // 리뷰 ID 목록 → {reviewId: likeCount} 맵 (쿼리 1번)
    private Map<Long, Long> buildLikeCountMap(List<Long> ids) {
        return reviewLikeRepository.countGroupByReviewIds(ids).stream()
                .collect(Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));
    }

    // 리뷰 ID 목록 → {reviewId: commentCount} 맵 (쿼리 1번)
    private Map<Long, Long> buildCommentCountMap(List<Long> ids) {
        return commentRepository.countGroupByReviewIds(ids).stream()
                .collect(Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Review findActiveReview(Long id) {
        return reviewRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Review findVisibleReview(Long id) {
        return reviewRepository.findByIdAndDeletedAtIsNullAndHiddenFalse(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private String normalizeWorkTitle(String title) {
        if (title == null) return "";
        return stripNotesPreservingVolumes(title)
                .replaceAll(EDITION_NOTE_PATTERN, " ")
                .replaceAll(EDITION_COLON_NOTE_PATTERN, " ")
                .replaceAll(ENGLISH_SUBTITLE_PATTERN, " ")
                .replaceAll(EDITION_SUFFIX_PATTERN, " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripNotesPreservingVolumes(String title) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(NOTE_PATTERN).matcher(title);
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            String note = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(normalized, isVolumeNote(note)
                    ? " " + java.util.regex.Matcher.quoteReplacement(matcher.group()) + " "
                    : " ");
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private boolean isVolumeNote(String note) {
        String value = note == null ? "" : note.replaceAll("\\s+", "");
        return value.matches("^(상|중|하)$") || value.matches("^(제)?\\d{1,2}(권|부|편|집|권째)?$");
    }

    private String normalizeAuthor(String author) {
        if (author == null) return "";
        return author
                .split("[,;/·]")[0]
                .replaceAll("\\s+", "")
                .trim();
    }
}
