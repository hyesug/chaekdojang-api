package com.chaekdojang.api.infra.wikidata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class WikidataBookCategoryClient {
    private static final String USER_AGENT = "Chaekdojang/1.0 (https://chaekdojang.com)";

    private final RestClient restClient;

    public WikidataBookCategoryClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl("https://www.wikidata.org")
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    public String findVerifiedCategory(String title, String author) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) return null;
        try {
            SearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/w/api.php")
                            .queryParam("action", "wbsearchentities")
                            .queryParam("search", title)
                            .queryParam("language", "ko")
                            .queryParam("uselang", "ko")
                            .queryParam("type", "item")
                            .queryParam("limit", 8)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);
            if (response == null || response.search() == null) return null;
            return response.search().stream()
                    .filter(item -> authorMatches(author, item.description()))
                    .map(item -> categoryFromDescription(item.description()))
                    .filter(category -> category != null)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("Wikidata 도서 분류 조회 실패: title={} message={}", title, exception.getMessage());
            return null;
        }
    }

    static boolean authorMatches(String author, String description) {
        if (author == null || description == null) return false;
        String normalizedDescription = normalize(description);
        List<String> tokens = List.of(author
                        .replaceAll("\\([^)]*\\)", " ")
                        .split("[\\s,;/·]+")).stream()
                .map(WikidataBookCategoryClient::normalize)
                .filter(token -> token.length() >= 2)
                .toList();
        if (tokens.isEmpty()) return false;
        long matches = tokens.stream().filter(normalizedDescription::contains).count();
        return matches >= Math.min(2, tokens.size());
    }

    static String categoryFromDescription(String description) {
        String value = normalize(description);
        if (value.isBlank()) return null;
        if (containsAny(value, "청소년소설", "youngadultnovel", "youngadultfiction")) return "청소년";
        if (containsAny(value, "아동소설", "어린이책", "childrensnovel", "childrensbook")) return "어린이(초등)";
        if (containsAny(value, "그래픽노블", "만화", "graphicnovel", "comicbook", "manga")) return "만화";
        if (containsAny(value, "소설", "novel", "fictionalwork")) return "소설";
        if (containsAny(value, "수필집", "에세이", "시집", "일기", "회고록", "essaycollection",
                "essay", "poetrycollection", "diary", "memoir")) return "시/에세이";
        if (containsAny(value, "철학서", "철학작품", "philosophicalwork", "philosophybook")) return "인문";
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^가-힣a-z0-9]", "");
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    record SearchResponse(List<SearchItem> search) {
    }

    record SearchItem(String id, String label, String description) {
    }
}
