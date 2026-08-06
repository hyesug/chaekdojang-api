package com.chaekdojang.api.infra.google;

import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.book.dto.BookSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GoogleBookClient {

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBookClient(@Value("${google.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://www.googleapis.com")
                .build();
        this.apiKey = apiKey;
    }

    public List<BookSearchResult> search(String query) {
        try {
            GoogleBookResponse response = restClient.get()
                    .uri("/books/v1/volumes?q={query}&maxResults=10&key={key}", query, apiKey)
                    .retrieve()
                    .body(GoogleBookResponse.class);

            if (response == null || response.items() == null) return Collections.emptyList();

            return response.items().stream()
                    .map(this::toSearchResult)
                    .filter(r -> r != null && r.isbn13() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Google Books API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private BookSearchResult toSearchResult(GoogleBookResponse.Item item) {
        GoogleBookResponse.VolumeInfo info = item.volumeInfo();
        if (info == null) return null;

        String isbn13 = extractIsbn13(info.industryIdentifiers());
        if (isbn13 == null) return null;

        String author = info.authors() != null && !info.authors().isEmpty()
                ? String.join(", ", info.authors()) : "";
        String thumbnail = info.imageLinks() != null ? info.imageLinks().thumbnail() : null;
        String category = info.categories() != null && !info.categories().isEmpty()
                ? info.categories().get(0) : null;

        return new BookSearchResult(isbn13, info.title(), author,
                info.publisher(), thumbnail, BookSource.GOOGLE_BOOKS, category, info.description());
    }

    private String extractIsbn13(List<GoogleBookResponse.IndustryIdentifier> identifiers) {
        if (identifiers == null) return null;
        return identifiers.stream()
                .filter(i -> "ISBN_13".equals(i.type()))
                .map(GoogleBookResponse.IndustryIdentifier::identifier)
                .findFirst()
                .orElse(null);
    }
}
