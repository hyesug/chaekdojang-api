package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.BookSearchResult;
import com.chaekdojang.api.domain.book.dto.PublicBookDetailResponse;
import com.chaekdojang.api.domain.review.Review;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.infra.google.GoogleBookClient;
import com.chaekdojang.api.infra.kakao.KakaoBookClient;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.domain.review.ReviewLikeRepository;
import com.chaekdojang.api.domain.review.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final GoogleBookClient googleBookClient;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public List<BookResponse> search(String query, String author, String publisher) {
        String searchQuery = buildSearchQuery(query, author, publisher);
        String titleFilter = normalizeSearchText(query);
        String authorFilter = normalizeSearchText(author);
        String publisherFilter = normalizeSearchText(publisher);
        if (searchQuery.isBlank()) return List.of();
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

        return books.values().stream()
                .map(this::toResponseWithReviewCount)
                .toList();
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
        return toResponseWithReviewCount(book);
    }

    @Transactional
    public PublicBookDetailResponse findPublicBySlug(String slug) {
        Book book = findPublicBook(slug);
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
                reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId()),
                reviewRepository.countReadersByBookId(book.getId()),
                excerpts,
                buildSentenceExcerpts(reviews));
    }

    @Transactional
    public List<BookResponse> findPublicBooksForSitemap() {
        Set<String> seen = new LinkedHashSet<>();
        return bookRepository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc()
                .stream()
                .filter(book -> seen.add(publicSlug(book)))
                .peek(this::ensureSeoFields)
                .map(this::toResponseWithReviewCount)
                .toList();
    }

    public List<BookResponse> findByCategory(String category) {
        return bookRepository.findAllByCategoryContainingIgnoreCase(category)
                .stream()
                .map(this::toResponseWithReviewCount)
                .toList();
    }

    private BookResponse toResponseWithReviewCount(Book book) {
        long reviewCount = reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId());
        return BookResponse.from(book, reviewCount);
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
        return bookRepository.findByIsbn13(result.isbn13())
                .orElseGet(() -> bookRepository.save(
                        Book.builder()
                                .isbn13(result.isbn13())
                                .title(result.title())
                                .author(result.author())
                                .publisher(result.publisher())
                                .thumbnail(result.thumbnail())
                                .slug(BookSlugGenerator.create(result.title(), result.author(), result.isbn13(), null))
                                .source(result.source())
                                .category(result.category())
                                .build()
                ));
    }

    private Book findPublicBook(String slug) {
        String normalized = slug == null ? "" : slug.trim();
        if (normalized.isBlank()) throw new CustomException(ErrorCode.BOOK_NOT_FOUND);
        return bookRepository.findFirstBySlugAndDeletedAtIsNullAndIsPublicTrueOrderByIdAsc(normalized)
                .or(() -> findKnownBookBySlug(normalized))
                .or(() -> findByNumericId(normalized))
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
            seoTitle = book.getTitle() + " 독후감과 문장 기록 | 책도장";
        }
        String seoDescription = book.getSeoDescription();
        if (seoDescription == null || seoDescription.isBlank()) {
            seoDescription = book.getAuthor() + "의 " + book.getTitle()
                    + "을 읽고 남긴 독후감, 인상 깊은 문장, 독서 기록을 책도장에서 확인해보세요.";
        }
        book.updateSeoFields(slug, description, seoTitle, seoDescription);
    }

    private String publicSlug(Book book) {
        if (book.getSlug() != null && !book.getSlug().isBlank()) return book.getSlug();
        return BookSlugGenerator.create(book.getTitle(), book.getAuthor(), book.getIsbn13(), book.getId());
    }

    private String defaultDescription(Book book) {
        return book.getTitle() + "은 " + book.getAuthor()
                + "의 책입니다. 책도장에서 이 책을 읽은 사람들의 독후감, 리뷰, 독서 기록과 인상 깊은 문장을 확인해보세요.";
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

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
