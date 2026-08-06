package com.chaekdojang.api.domain.book;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.BookReactionReportResponse;
import com.chaekdojang.api.domain.book.dto.BookConnectionResponse;
import com.chaekdojang.api.domain.book.dto.BookSearchResult;
import com.chaekdojang.api.domain.book.dto.PublicBookDetailResponse;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummary;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryRepository;
import com.chaekdojang.api.domain.review.ai.ReviewAiSummaryStatus;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.infra.google.GoogleBookClient;
import com.chaekdojang.api.infra.kakao.KakaoBookClient;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.review.ReviewLikeRepository;
import com.chaekdojang.api.domain.review.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final GoogleBookClient googleBookClient;
    private final WebNovelService webNovelService;
    private final BookCategoryResolver bookCategoryResolver;
    private final ReviewRepository reviewRepository;
    private final ReviewAiSummaryRepository reviewAiSummaryRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<BookResponse> search(String query, String author, String publisher) {
        String searchQuery = buildSearchQuery(query, author, publisher);
        String titleFilter = normalizeSearchText(query);
        String authorFilter = normalizeSearchText(author);
        String publisherFilter = normalizeSearchText(publisher);
        if (searchQuery.isBlank()) return List.of();
        String cacheKey = bookSearchCacheKey(titleFilter, authorFilter, publisherFilter);
        List<BookResponse> cached = readBookSearchCache(cacheKey);
        if (cached != null) return cached;

        List<BookSearchResult> results = new ArrayList<>();
        results.addAll(kakaoBookClient.search(searchQuery));
        results.addAll(googleBookClient.search(searchQuery));

        Map<String, Book> books = new LinkedHashMap<>();
        for (Book book : bookRepository.searchByFilters(titleFilter, authorFilter, publisherFilter)) {
            books.putIfAbsent(bookKey(book), book);
        }

        for (BookSearchResult r : results) {
            if (!matchesFilters(r, titleFilter, authorFilter, publisherFilter)) continue;
            Book book = upsertBook(r);
            books.putIfAbsent(bookKey(book), book);
        }

        Map<Book, String> categories = bookCategoryResolver.resolveAll(books.values());
        List<BookResponse> responses = books.values().stream()
                .map(book -> toResponseWithReviewCount(book, categories.get(book)))
                .toList();
        writeBookSearchCache(cacheKey, responses);
        return responses;
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        return toResponseWithReviewCount(book);
    }

    public BookReactionReportResponse getReactionReport(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        List<Review> reviews = reviewRepository.findAllByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(bookId);
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, ReviewAiSummary> summaryMap = reviewIds.isEmpty()
                ? Map.of()
                : reviewAiSummaryRepository.findAllByReviewIdIn(reviewIds)
                .stream()
                .filter(this::isCompletedSummary)
                .collect(Collectors.toMap(summary -> summary.getReview().getId(), Function.identity()));
        List<BookReactionReportResponse.ReviewCardInfo> cards = reviews.stream()
                .map(review -> {
                    ReviewAiSummary summary = summaryMap.get(review.getId());
                    return summary == null ? null : BookReactionReportResponse.ReviewCardInfo.of(review, summary);
                })
                .filter(card -> card != null)
                .toList();
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .stream()
                .map(value -> Math.round(value * 10.0) / 10.0)
                .findFirst()
                .orElse(0.0);
        long participantCount = reviews.stream().map(review -> review.getAuthor().getId()).distinct().count();
        int minimumAggregateReviewCount = 3;
        boolean aggregateAvailable = reviews.size() >= minimumAggregateReviewCount
                && participantCount >= minimumAggregateReviewCount;
        Map<String, Long> reviewKeywordCounts = reviews.stream()
                .filter(review -> review.getKeywords() != null)
                .flatMap(review -> java.util.Arrays.stream(review.getKeywords().split(",")))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<BookReactionReportResponse.KeywordStat> commonReviewKeywords = aggregateAvailable
                ? reviewKeywordCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(8)
                .map(entry -> new BookReactionReportResponse.KeywordStat(entry.getKey(), entry.getValue()))
                .toList()
                : List.of();
        long positive = reviews.stream().filter(review -> review.getRating() >= 4).count();
        long negative = reviews.stream().filter(review -> review.getRating() <= 2).count();
        long neutral = reviews.size() - positive - negative;
        List<String> perspectiveNotes = new ArrayList<>();
        if (aggregateAvailable) {
            commonReviewKeywords.forEach(keyword -> perspectiveNotes.add(
                    "등록된 독후감 중 " + keyword.count() + "개에서 #" + keyword.keyword() + " 키워드가 선택되었습니다."));
            if (positive > 0 && negative > 0) {
                perspectiveNotes.add("높은 별점과 낮은 별점이 함께 있어 독자들의 반응이 한쪽으로만 모이지 않았습니다.");
            }
        }
        return new BookReactionReportResponse(
                BookReactionReportResponse.BookInfo.from(book),
                reviews.size(),
                participantCount,
                averageRating,
                aggregateAvailable ? commonEmotionKeywords(cards) : List.of(),
                cards.isEmpty() ? null : cards.get(0).oneLineReview(),
                cards.isEmpty() ? null : cards.get(0).recommendedFor(),
                cards.isEmpty() ? null : cards.get(0).impressivePoint(),
                cards,
                aggregateAvailable,
                minimumAggregateReviewCount,
                commonReviewKeywords,
                aggregateAvailable
                        ? new BookReactionReportResponse.RatingDistribution(positive, neutral, negative)
                        : new BookReactionReportResponse.RatingDistribution(0, 0, 0),
                perspectiveNotes
        );
    }

    public List<BookConnectionResponse> getConnections(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        }
        List<Object[]> stats = reviewRepository.findConnectedBookStats(bookId);
        if (stats.isEmpty()) return List.of();
        Map<Long, Book> books = bookRepository.findAllById(
                        stats.stream().map(row -> toLong(row[0])).toList())
                .stream()
                .filter(book -> book.getDeletedAt() == null && book.isPublic())
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return stats.stream()
                .map(row -> {
                    Long connectedBookId = toLong(row[0]);
                    long sharedReaders = toLong(row[1]);
                    Book book = books.get(connectedBookId);
                    if (book == null) return null;
                    return new BookConnectionResponse(
                            book.getId(), book.getTitle(), book.getAuthor(), book.getThumbnail(), sharedReaders,
                            "이 책을 기록한 독자 " + sharedReaders + "명이 함께 기록했습니다.");
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public PublicBookDetailResponse findPublicBySlug(String slug) {
        Book book = findPublicBook(slug);
        refreshDescriptionIfNeeded(book);
        ensureSeoFields(book);

        List<Review> reviews = reviewRepository.findTop5ByBookIdAndDeletedAtIsNullAndHiddenFalseOrderByCreatedAtDesc(book.getId());
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, Long> likeMap = buildLikeCountMap(ids);
        Map<Long, Long> commentMap = buildCommentCountMap(ids);
        List<PublicBookDetailResponse.ReviewExcerpt> excerpts = reviews.stream()
                .map(review -> PublicBookDetailResponse.ReviewExcerpt.from(
                        review,
                        likeMap.getOrDefault(review.getId(), 0L),
                        commentMap.getOrDefault(review.getId(), 0L)))
                .toList();

        return PublicBookDetailResponse.from(
                book,
                synopsis(book),
                reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId()),
                reviewRepository.countReadersByBookId(book.getId()),
                excerpts,
                buildSentenceExcerpts(reviews));
    }

    @Transactional
    public List<BookResponse> findPublicBooksForSitemap() {
        Set<String> seen = new LinkedHashSet<>();
        List<Book> books = bookRepository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc()
                .stream()
                .filter(book -> seen.add(publicSlug(book)))
                .peek(this::ensureSeoFields)
                .toList();
        Map<Book, String> categories = bookCategoryResolver.resolveAll(books);
        return books.stream()
                .map(book -> toResponseWithReviewCount(book, categories.get(book)))
                .toList();
    }

    public List<BookResponse> findByCategory(String category) {
        List<Book> books = bookRepository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc();
        Map<Book, String> categories = bookCategoryResolver.resolveAll(books);
        return books.stream()
                .filter(book -> category.equalsIgnoreCase(categories.get(book)))
                .map(book -> toResponseWithReviewCount(book, categories.get(book)))
                .toList();
    }

    private BookResponse toResponseWithReviewCount(Book book) {
        return toResponseWithReviewCount(book, bookCategoryResolver.resolve(book));
    }

    private BookResponse toResponseWithReviewCount(Book book, String category) {
        long reviewCount = reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId());
        return BookResponse.from(book, reviewCount, category);
    }

    private String buildSearchQuery(String query, String author, String publisher) {
        String title = query == null ? "" : query.trim();
        String writer = author == null ? "" : author.trim();
        String publisherName = publisher == null ? "" : publisher.trim();
        StringBuilder builder = new StringBuilder(title);
        if (!writer.isBlank()) builder.append(" ").append(writer);
        if (!publisherName.isBlank()) builder.append(" ").append(publisherName);
        return builder.toString();
    }

    private String bookSearchCacheKey(String title, String author, String publisher) {
        return "book-search:v2:" + title + ":" + author + ":" + publisher;
    }

    private List<BookResponse> readBookSearchCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) return null;
            return objectMapper.readValue(value, new TypeReference<List<BookResponse>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeBookSearchCache(String key, List<BookResponse> responses) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(responses), Duration.ofHours(6));
        } catch (Exception ignored) {
            // Redis 캐시 실패가 책 검색 자체를 막지 않도록 무시한다.
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    private boolean isIsbnQuery(String value) {
        return value.matches("\\d{10,13}");
    }

    private boolean matchesFilters(BookSearchResult result, String title, String author, String publisher) {
        if (!title.isBlank() && !isIsbnQuery(title) && !normalizeSearchText(result.title()).contains(title)) {
            return false;
        }
        if (!author.isBlank() && !normalizeSearchText(result.author()).contains(author)) {
            return false;
        }
        return publisher.isBlank() || normalizeSearchText(result.publisher()).contains(publisher);
    }

    private String bookKey(Book book) {
        if (book.getIsbn13() != null && !book.getIsbn13().isBlank()) {
            return book.getIsbn13();
        }
        return "id:" + book.getId();
    }

    private Book upsertBook(BookSearchResult result) {
        String isbn13 = normalizeIsbn13(result.isbn13());
        if (isbn13 != null) {
            return bookRepository.findByIsbn13(isbn13)
                    .map(book -> updateBookMetadata(book, result))
                    .orElseGet(() -> createBook(result, isbn13));
        }
        return createBook(result, null);
    }

    private Book createBook(BookSearchResult result, String isbn13) {
        return bookRepository.save(
                Book.builder()
                        .isbn13(isbn13)
                        .title(result.title())
                        .author(result.author())
                        .publisher(result.publisher())
                        .thumbnail(result.thumbnail())
                        .description(normalizeDescription(result.description()))
                        .slug(BookSlugGenerator.create(result.title(), result.author(), isbn13, null))
                        .source(result.source())
                        .category(BookGenreClassifier.resolve(
                                result.category(), result.source(), result.title(), result.author(), result.description()))
                        .build()
        );
    }

    private Book updateDescriptionIfNeeded(Book book, String candidate) {
        String normalized = normalizeDescription(candidate);
        if (needsDescription(book) && isBetterDescription(book.getDescription(), normalized)) {
            book.updateDescription(normalized);
        }
        return book;
    }

    private Book updateBookMetadata(Book book, BookSearchResult result) {
        updateDescriptionIfNeeded(book, result.description());
        if (result.category() != null && !result.category().isBlank()) {
            String category = BookGenreClassifier.resolve(
                    result.category(), result.source(), result.title(), result.author(), result.description());
            if (category != null && !category.equals(BookGenreClassifier.resolve(book))) {
                book.updateCategory(category);
            }
        }
        return book;
    }

    private void refreshDescriptionIfNeeded(Book book) {
        if (!needsDescription(book)) return;
        String missKey = book.isWebNovel()
                ? "book:web-novel-description:miss:v2:" + book.getId()
                : "book:description:miss:" + book.getId();
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(missKey))) return;
        } catch (RuntimeException ignored) {
            // Redis 장애가 책 상세 조회를 막지 않도록 외부 도서 API 조회를 계속한다.
        }
        if (book.isWebNovel()) {
            String description = normalizeDescription(webNovelService.findDescription(book));
            if (description != null
                    && (book.getDescription() == null
                    || !looksTruncated(description)
                    || description.length() > book.getDescription().length())) {
                book.updateDescription(description);
                return;
            }
            cacheDescriptionMiss(missKey);
            return;
        }
        String query = book.getIsbn13() != null && !book.getIsbn13().isBlank()
                ? book.getIsbn13()
                : book.getTitle() + " " + book.getAuthor();
        List<BookSearchResult> candidates = new ArrayList<>();
        candidates.addAll(kakaoBookClient.search(query));
        candidates.addAll(googleBookClient.search(query));
        BookSearchResult matched = candidates.stream()
                .filter(candidate -> normalizeDescription(candidate.description()) != null)
                .filter(candidate -> sameBook(book, candidate))
                .max(Comparator.comparingInt(candidate ->
                        BookDescriptionPolicy.qualityScore(normalizeDescription(candidate.description()))))
                .orElse(null);
        String refreshed = matched == null ? null : normalizeDescription(matched.description());
        if (isBetterDescription(book.getDescription(), refreshed)) {
            book.updateDescription(refreshed);
            return;
        }
        cacheDescriptionMiss(missKey);
    }

    private void cacheDescriptionMiss(String missKey) {
        try {
            redisTemplate.opsForValue().set(missKey, "1", Duration.ofHours(12));
        } catch (RuntimeException ignored) {
            // 조회 실패 캐시는 선택 사항이다.
        }
    }

    private boolean sameBook(Book book, BookSearchResult candidate) {
        String bookIsbn = normalizeIsbn13(book.getIsbn13());
        String candidateIsbn = normalizeIsbn13(candidate.isbn13());
        if (bookIsbn != null && candidateIsbn != null) return bookIsbn.equals(candidateIsbn);
        return normalizeSearchText(book.getTitle()).equals(normalizeSearchText(candidate.title()));
    }

    private String synopsis(Book book) {
        return isMissingDescription(book)
                ? null
                : BookDescriptionPolicy.displaySynopsis(book.getDescription(), book.isWebNovel());
    }

    private boolean needsDescription(Book book) {
        String description = book.getDescription();
        return isMissingDescription(book)
                || description.length() == 2000
                || (book.isWebNovel() && looksTruncated(description))
                || (!book.isWebNovel() && BookDescriptionPolicy.looksAbruptlyTruncated(description));
    }

    private boolean isMissingDescription(Book book) {
        String description = book.getDescription();
        return description == null || description.isBlank()
                || description.contains("책도장에서 이 책을 읽은 사람들의 독후감")
                || description.contains("책도장에서 이 작품을 읽은 사람들의 감상");
    }

    private boolean looksTruncated(String description) {
        String normalized = description == null ? "" : description.trim();
        return normalized.endsWith("...") || normalized.endsWith("…");
    }

    private boolean isBetterDescription(String current, String candidate) {
        return candidate != null
                && BookDescriptionPolicy.qualityScore(candidate) > BookDescriptionPolicy.qualityScore(current);
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) return null;
        String withoutTags = value.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]+>", " ");
        String normalized = HtmlUtils.htmlUnescape(withoutTags).replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) return null;
        return normalized;
    }

    private String normalizeIsbn13(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[^0-9Xx]", "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Book findPublicBook(String slug) {
        String normalized = slug == null ? "" : slug.trim();
        if (normalized.isBlank()) throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        if (normalized.matches("\\d+")) {
            return findByNumericId(normalized)
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        }
        return bookRepository.findFirstBySlugAndDeletedAtIsNullAndIsPublicTrueOrderByIdAsc(normalized)
                .or(() -> findKnownBookBySlug(normalized))
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
    }

    private java.util.Optional<Book> findKnownBookBySlug(String slug) {
        return switch (slug) {
            case "demian" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("데미안");
            case "human-disqualification" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("인간");
            case "the-stranger" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("이방인");
            case "siddhartha" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("싯다르타");
            case "stoner" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("스토너");
            case "inconvenient-convenience-store" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("불편한 편의점");
            case "mosun" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("모순");
            case "the-long-long-night" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("긴긴밤");
            case "human-acts" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("소년이 온다");
            case "why-fish-dont-exist" -> bookRepository.findFirstByDeletedAtIsNullAndIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdAsc("물고기는 존재하지 않는다");
            default -> java.util.Optional.empty();
        };
    }

    private java.util.Optional<Book> findByNumericId(String slug) {
        if (!slug.matches("\\d+")) return java.util.Optional.empty();
        return bookRepository.findById(Long.parseLong(slug))
                .filter(book -> book.getDeletedAt() == null && book.isPublic());
    }

    private void ensureSeoFields(Book book) {
        String slug = book.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = BookSlugGenerator.create(book.getTitle(), book.getAuthor(), book.getIsbn13(), book.getId());
        }
        String description = book.getDescription();
        if (description == null || description.isBlank()) {
            description = defaultDescription(book);
        }
        String seoTitle = book.getSeoTitle();
        if (seoTitle == null || seoTitle.isBlank()) {
            seoTitle = book.isWebNovel()
                    ? book.getTitle() + " 웹소설 감상과 독후감 | 책도장"
                    : book.getTitle() + " 독후감과 문장 기록 | 책도장";
        }
        String seoDescription = book.getSeoDescription();
        if (seoDescription == null || seoDescription.isBlank()) {
            String authorPrefix = book.getAuthor() == null || book.getAuthor().isBlank()
                    ? ""
                    : book.getAuthor() + "의 ";
            seoDescription = book.isWebNovel()
                    ? authorPrefix + book.getTitle() + "을 읽고 남긴 웹소설 감상과 독후감을 책도장에서 확인해보세요."
                    : authorPrefix + book.getTitle()
                    + "을 읽고 남긴 독후감, 인상 깊은 문장, 독서 기록을 책도장에서 확인해보세요.";
        }
        book.updateSeoFields(slug, description, seoTitle, seoDescription);
    }

    private String publicSlug(Book book) {
        if (book.getSlug() != null && !book.getSlug().isBlank()) return book.getSlug();
        return BookSlugGenerator.create(book.getTitle(), book.getAuthor(), book.getIsbn13(), book.getId());
    }

    private String defaultDescription(Book book) {
        String authorText = book.getAuthor() == null || book.getAuthor().isBlank()
                ? ""
                : book.getAuthor() + "의 ";
        if (book.isWebNovel()) {
            return book.getTitle() + "은 " + authorText
                    + "웹소설입니다. 책도장에서 이 작품을 읽은 사람들의 감상과 독후감을 확인해보세요.";
        }
        return book.getTitle() + "은 " + authorText
                + "책입니다. 책도장에서 이 책을 읽은 사람들의 독후감, 리뷰, 독서 기록과 인상 깊은 문장을 확인해보세요.";
    }

    private List<String> buildSentenceExcerpts(List<Review> reviews) {
        return reviews.stream()
                .map(Review::getContent)
                .map(this::firstReadableSentence)
                .filter(sentence -> !sentence.isBlank())
                .limit(5)
                .toList();
    }

    private String firstReadableSentence(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 140) return normalized;
        return normalized.substring(0, 140) + "...";
    }

    private Map<Long, Long> buildLikeCountMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return reviewLikeRepository.countGroupByReviewIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));
    }

    private Map<Long, Long> buildCommentCountMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return commentRepository.countGroupByReviewIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(row -> toLong(row[0]), row -> toLong(row[1])));
    }

    private boolean isCompletedSummary(ReviewAiSummary summary) {
        return summary.getStatus() == ReviewAiSummaryStatus.COMPLETED
                || summary.getStatus() == ReviewAiSummaryStatus.EDITED;
    }

    private List<String> commonEmotionKeywords(List<BookReactionReportResponse.ReviewCardInfo> cards) {
        return cards.stream()
                .flatMap(card -> card.emotionKeywords().stream())
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
