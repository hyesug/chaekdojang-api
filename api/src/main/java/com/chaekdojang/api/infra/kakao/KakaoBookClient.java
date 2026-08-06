package com.chaekdojang.api.infra.kakao;

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
public class KakaoBookClient {

    private final RestClient restClient;

    public KakaoBookClient(@Value("${kakao.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }

    public List<BookSearchResult> search(String query) {
        try {
            KakaoBookResponse response = restClient.get()
                    .uri("/v3/search/book?query={query}&size=10", query)
                    .retrieve()
                    .body(KakaoBookResponse.class);

            if (response == null || response.documents() == null) return Collections.emptyList();

            return response.documents().stream()
                    .map(this::toSearchResult)
                    .filter(r -> r != null && r.isbn13() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Kakao Books API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private BookSearchResult toSearchResult(KakaoBookResponse.Document doc) {
        String isbn13 = extractIsbn13(doc.isbn());
        if (isbn13 == null) return null;

        String author = doc.authors() != null && !doc.authors().isEmpty()
                ? String.join(", ", doc.authors()) : "";

        return new BookSearchResult(isbn13, doc.title(), author,
                doc.publisher(), doc.thumbnail(), BookSource.KAKAO, null, doc.contents());
    }

    private String extractIsbn13(String isbn) {
        if (isbn == null || isbn.isBlank()) return null;
        for (String part : isbn.strip().split(" ")) {
            if (part.length() == 13) return part;
        }
        return null;
    }
}
