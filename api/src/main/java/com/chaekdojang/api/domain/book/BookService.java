package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.BookSearchResult;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.infra.google.GoogleBookClient;
import com.chaekdojang.api.infra.kakao.KakaoBookClient;
import com.chaekdojang.api.domain.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final GoogleBookClient googleBookClient;
    private final ReviewRepository reviewRepository;

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
                                .source(result.source())
                                .category(result.category())
                                .build()
                ));
    }
}
