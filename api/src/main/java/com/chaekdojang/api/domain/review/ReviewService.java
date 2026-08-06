package com.chaekdojang.api.domain.review;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.chat.ChatBlockRepository;
import com.chaekdojang.api.domain.library.Library;
import com.chaekdojang.api.domain.library.LibraryRepository;
import com.chaekdojang.api.domain.library.LibraryStatus;
import com.chaekdojang.api.domain.metrics.MetricEventService;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.review.dto.ReviewCreateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewContinuationResponse;
import com.chaekdojang.api.domain.review.dto.ReviewRereadHistoryResponse;
import com.chaekdojang.api.domain.review.dto.ReviewResponse;
import com.chaekdojang.api.domain.review.dto.ReviewUpdateRequest;
import com.chaekdojang.api.domain.review.dto.ReviewVisibilityRequest;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummary;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryRepository;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ChatBlockRepository chatBlockRepository;
    private final LibraryRepository libraryRepository;
    private final NotificationService notificationService;
    private final ReviewAiSummaryRepository reviewAiSummaryRepository;
    private final ReviewAiSummaryService reviewAiSummaryService;
    private final MetricEventService metricEventService;

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
        if (request.previousReviewId() != null && request.sourceReviewId() != null) {
            throw new CustomException(ErrorCode.INVALID_REVIEW_LINK);
        }
        Review previousReview = resolvePreviousReview(request.previousReviewId(), userId, book);
        Review sourceReview = resolveSourceReview(request.sourceReviewId(), userId, book);
        Review review = Review.builder()
                .author(author).book(book).previousReview(previousReview).sourceReview(sourceReview)
                .content(request.content()).rating(request.rating())
                .keywords(normalizeKeywords(request.keywords())).spoiler(request.hasSpoiler())
                .build();
        if (request.shouldHide()) {
            review.hide();
        }
        ReviewResponse saved = ReviewResponse.from(saveReview(review), 0L, 0L);
        if (request.shouldGenerateAiSummary()) {
            reviewAiSummaryService.enqueueForReview(review);
        }

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
            if (!review.isHidden()) {
                notifySameBookReaders(author, finalBook, review.getId(),
                        sourceReview != null ? sourceReview.getAuthor().getId() : null);
            }
        }
        if (sourceReview != null && !review.isHidden()) {
            notificationService.send(
                    sourceReview.getAuthor(), author, NotificationType.REVIEW_CONTINUED, review.getId());
        }

        Map<String, Object> activityMeta = new java.util.LinkedHashMap<>();
        activityMeta.put("reviewId", saved.id());
        if (finalBook != null) {
            activityMeta.put("bookId", finalBook.getId());
            activityMeta.put("bookTitle", finalBook.getTitle());
        }
        if (previousReview != null) {
            activityMeta.put("previousReviewId", previousReview.getId());
        }
        if (sourceReview != null) {
            activityMeta.put("sourceReviewId", sourceReview.getId());
        }
        metricEventService.recordCurrentRequestEvent(
                "review_created", userId, "/reviews/" + saved.id(), activityMeta);

        return saved;
    }

    private void notifySameBookReaders(User author, Book book, Long reviewId, Long excludedReaderId) {
        reviewRepository.findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(book.getId())
                .stream()
                .map(Review::getAuthor)
                .filter(reader -> reader != null && reader.getDeletedAt() == null)
                .filter(reader -> !reader.getId().equals(author.getId()))
                .filter(reader -> excludedReaderId == null || !reader.getId().equals(excludedReaderId))
                .collect(Collectors.toMap(User::getId, reader -> reader, (first, ignored) -> first))
                .values()
                .stream()
                .limit(50)
                .forEach(reader -> notificationService.send(reader, author, NotificationType.SAME_BOOK_REVIEW, reviewId));
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

    public ReviewRereadHistoryResponse getRereadHistory(Long id) {
        Review current = findActiveReview(id);
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean canSeePrivate = current.isAuthor(currentUserId)
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
        if (current.isHidden() && !canSeePrivate) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }

        List<Review> candidates = current.getBook() == null
                ? List.of(current)
                : reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        current.getAuthor().getId(), current.getBook().getId());
        Long rootId = rootReviewId(current);
        List<Review> chain = candidates.stream()
                .filter(review -> rootId.equals(rootReviewId(review)))
                .sorted(Comparator.comparing(Review::getCreatedAt).thenComparing(Review::getId))
                .toList();
        List<Review> visibleChain = canSeePrivate
                ? chain
                : chain.stream().filter(review -> !review.isHidden()).toList();
        Review latest = chain.stream()
                .max(Comparator.comparing(Review::getCreatedAt).thenComparing(Review::getId))
                .orElse(current);

        List<ReviewRereadHistoryResponse.HistoryItem> records = visibleChain.stream()
                .map(review -> new ReviewRereadHistoryResponse.HistoryItem(
                        review.getId(), rereadSequence(review), review.getRating(), review.isHidden(),
                        review.getId().equals(current.getId()), review.getCreatedAt()))
                .toList();
        return new ReviewRereadHistoryResponse(
                records,
                latest.getId(),
                current.isAuthor(currentUserId));
    }

    public ReviewContinuationResponse getContinuations(Long id) {
        Review current = findActiveReview(id);
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean canSeePrivate = current.isAuthor(currentUserId)
                || SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN");
        if (current.isHidden() && !canSeePrivate) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }

        Review source = current.getSourceReview();
        boolean sourceVisible = source != null
                && source.getDeletedAt() == null
                && !source.isHidden()
                && source.getAuthor().getDeletedAt() == null
                && !isBlockedBetween(currentUserId, source.getAuthor().getId());
        ReviewContinuationResponse.SourceReview sourceResponse = sourceVisible
                ? new ReviewContinuationResponse.SourceReview(
                        source.getId(), source.getAuthor().getId(), source.getAuthor().getNickname(), source.getCreatedAt())
                : null;

        List<ReviewContinuationResponse.ContinuationItem> continuations =
                reviewRepository.findAllBySourceReviewIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(id)
                        .stream()
                        .filter(review -> review.getAuthor().getDeletedAt() == null)
                        .filter(review -> !isBlockedBetween(currentUserId, review.getAuthor().getId()))
                        .map(review -> new ReviewContinuationResponse.ContinuationItem(
                                review.getId(), review.getAuthor().getId(), review.getAuthor().getNickname(),
                                excerpt(review.getContent()), review.getCreatedAt()))
                        .toList();

        boolean canContinue = currentUserId != null
                && !current.isHidden()
                && !current.isAuthor(currentUserId)
                && current.getBook() != null
                && current.getAuthor().getDeletedAt() == null
                && !isBlockedBetween(currentUserId, current.getAuthor().getId());
        return new ReviewContinuationResponse(
                sourceResponse, source != null && !sourceVisible,
                continuations, continuations.size(), canContinue);
    }

    @Transactional
    public ReviewResponse update(Long id, ReviewUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Review review = findActiveReview(id);
        if (!review.isAuthor(userId)) throw new CustomException(ErrorCode.FORBIDDEN);
        Book book = review.getBook();
        if (request.bookId() != null) {
            book = bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        }
        validateRereadBookChange(review, book);
        review.update(request.content(), request.rating(), book);
        review.updateDiscoveryMetadata(normalizeKeywords(request.keywords()), request.hasSpoiler());
        if (request.shouldHide()) review.hide(); else review.unhide();
        if (request.shouldGenerateAiSummary()) {
            reviewAiSummaryService.enqueueForReview(review);
        }
        final Book updatedBook = book;
        if (updatedBook != null) {
            libraryRepository.findByUserIdAndBookId(userId, updatedBook.getId())
                    .ifPresentOrElse(
                            lib -> lib.updateStatus(LibraryStatus.FINISHED, null),
                            () -> libraryRepository.save(
                                    Library.builder().user(review.getAuthor()).book(updatedBook).status(LibraryStatus.FINISHED).build()
                            )
                    );
        }
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
        return getByBook(bookId, sort, "all", "all", null);
    }

    public List<ReviewResponse> getByBook(
            Long bookId, String sort, String length, String spoiler, String keyword) {
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
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return toResponseList(reviews.stream()
                .filter(review -> switch (length) {
                    case "short" -> review.getContent().length() < 300;
                    case "long" -> review.getContent().length() >= 300;
                    default -> true;
                })
                .filter(review -> switch (spoiler) {
                    case "exclude" -> !review.isSpoiler();
                    case "only" -> review.isSpoiler();
                    default -> true;
                })
                .filter(review -> normalizedKeyword.isBlank()
                        || containsKeyword(review.getKeywords(), normalizedKeyword))
                .toList());
    }

    private boolean containsKeyword(String storedKeywords, String keyword) {
        if (storedKeywords == null || storedKeywords.isBlank()) return false;
        return java.util.Arrays.stream(storedKeywords.split(","))
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(keyword));
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
        Map<Long, ReviewAiSummary> summaryMap = buildSummaryMap(ids);
        return page.map(r -> ReviewResponse.from(r,
                likeMap.getOrDefault(r.getId(), 0L),
                commentMap.getOrDefault(r.getId(), 0L),
                summaryMap.get(r.getId())));
    }

    private List<ReviewResponse> toResponseList(List<Review> reviews) {
        if (reviews.isEmpty()) return List.of();
        List<Review> limited = reviews.stream().limit(100).toList();
        List<Long> ids = limited.stream().map(Review::getId).toList();
        Map<Long, Long> likeMap = buildLikeCountMap(ids);
        Map<Long, Long> commentMap = buildCommentCountMap(ids);
        Map<Long, ReviewAiSummary> summaryMap = buildSummaryMap(ids);
        return limited.stream()
                .map(r -> ReviewResponse.from(r,
                        likeMap.getOrDefault(r.getId(), 0L),
                        commentMap.getOrDefault(r.getId(), 0L),
                        summaryMap.get(r.getId())))
                .toList();
    }

    private Map<Long, ReviewAiSummary> buildSummaryMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return reviewAiSummaryRepository.findAllByReviewIdIn(ids).stream()
                .collect(Collectors.toMap(summary -> summary.getReview().getId(), summary -> summary));
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

    private Review resolvePreviousReview(Long previousReviewId, Long userId, Book book) {
        if (previousReviewId == null) return null;
        Review previous = findActiveReview(previousReviewId);
        if (!previous.isAuthor(userId)
                || previous.getBook() == null
                || book == null
                || !previous.getBook().getId().equals(book.getId())) {
            throw new CustomException(ErrorCode.INVALID_REREAD_SOURCE);
        }
        if (reviewRepository.existsByPreviousReviewIdAndDeletedAtIsNull(previousReviewId)
                || hasActiveDescendant(previous, book)) {
            throw new CustomException(ErrorCode.REREAD_ALREADY_EXISTS);
        }
        return previous;
    }

    private Review resolveSourceReview(Long sourceReviewId, Long userId, Book book) {
        if (sourceReviewId == null) return null;
        Review source = findActiveReview(sourceReviewId);
        if (source.isHidden()
                || source.isAuthor(userId)
                || source.getBook() == null
                || book == null
                || !source.getBook().getId().equals(book.getId())
                || source.getAuthor().getDeletedAt() != null) {
            throw new CustomException(ErrorCode.INVALID_CONTINUATION_SOURCE);
        }
        if (isBlockedBetween(userId, source.getAuthor().getId())) {
            throw new CustomException(ErrorCode.BLOCKED_REVIEW_CONNECTION);
        }
        return source;
    }

    private void validateRereadBookChange(Review review, Book newBook) {
        Long currentBookId = review.getBook() != null ? review.getBook().getId() : null;
        Long newBookId = newBook != null ? newBook.getId() : null;
        if (java.util.Objects.equals(currentBookId, newBookId)) return;
        if (review.getPreviousReview() != null
                || review.getSourceReview() != null
                || reviewRepository.existsByPreviousReviewIdAndDeletedAtIsNull(review.getId())
                || hasActiveDescendant(review, review.getBook())
                || reviewRepository.existsBySourceReviewIdAndDeletedAtIsNull(review.getId())) {
            throw new CustomException(ErrorCode.CONNECTED_REVIEW_BOOK_CHANGE_NOT_ALLOWED);
        }
    }

    private boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) return false;
        return chatBlockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
                firstUserId, secondUserId, secondUserId, firstUserId);
    }

    private String excerpt(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), 120));
    }

    private String normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        String value = keywords.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .map(keyword -> keyword.replace(",", ""))
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .limit(10)
                .map(keyword -> keyword.substring(0, Math.min(keyword.length(), 30)))
                .collect(Collectors.joining(","));
        return value.isBlank() ? null : value;
    }

    private boolean hasActiveDescendant(Review source, Book book) {
        if (book == null) return false;
        return reviewRepository.findAllByAuthorIdAndBookIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        source.getAuthor().getId(), book.getId()).stream()
                .anyMatch(candidate -> !candidate.getId().equals(source.getId())
                        && hasAncestor(candidate, source.getId()));
    }

    private boolean hasAncestor(Review review, Long ancestorId) {
        Review cursor = review.getPreviousReview();
        Set<Long> visited = new HashSet<>();
        while (cursor != null && visited.add(cursor.getId())) {
            if (cursor.getId().equals(ancestorId)) return true;
            cursor = cursor.getPreviousReview();
        }
        return false;
    }

    private Review saveReview(Review review) {
        if (review.getPreviousReview() == null) return reviewRepository.save(review);
        try {
            return reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.REREAD_ALREADY_EXISTS);
        }
    }

    private Long rootReviewId(Review review) {
        Review cursor = review;
        Set<Long> visited = new HashSet<>();
        while (cursor.getPreviousReview() != null && visited.add(cursor.getId())) {
            cursor = cursor.getPreviousReview();
        }
        return cursor.getId();
    }

    private int rereadSequence(Review review) {
        int sequence = 1;
        Review cursor = review;
        Set<Long> visited = new HashSet<>();
        while (cursor.getPreviousReview() != null && visited.add(cursor.getId())) {
            sequence++;
            cursor = cursor.getPreviousReview();
        }
        return sequence;
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
