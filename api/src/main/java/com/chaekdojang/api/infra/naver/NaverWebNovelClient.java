package com.chaekdojang.api.infra.naver;

import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class NaverWebNovelClient {

    private final RestClient restClient;
    private final boolean configured;

    public NaverWebNovelClient(
            @Value("${naver.search.client-id:}") String clientId,
            @Value("${naver.search.client-secret:}") String clientSecret
    ) {
        this.configured = !clientId.isBlank() && !clientSecret.isBlank();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openapi.naver.com");
        if (configured) {
            builder.defaultHeader("X-Naver-Client-Id", clientId)
                    .defaultHeader("X-Naver-Client-Secret", clientSecret);
        }
        this.restClient = builder.build();
    }

    public List<WebNovelSearchResult> search(String query) {
        if (!configured) return List.of();

        Map<String, WebNovelSearchResult> results = new LinkedHashMap<>();
        for (WebNovelPlatform platform : WebNovelPlatform.values()) {
            for (WebNovelSearchResult result : searchPlatform(query, platform)) {
                results.putIfAbsent(result.platform().name() + ":" + result.externalId(), result);
            }
        }
        return new ArrayList<>(results.values());
    }

    private List<WebNovelSearchResult> searchPlatform(String query, WebNovelPlatform platform) {
        try {
            NaverWebSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/v1/search/webkr.json")
                            .queryParam("query", platform.searchQuery(query))
                            .queryParam("display", 20)
                            .build())
                    .retrieve()
                    .body(NaverWebSearchResponse.class);
            if (response == null || response.items() == null) return List.of();

            return response.items().stream()
                    .map(item -> toResult(query, platform, item))
                    .filter(result -> result != null)
                    .limit(5)
                    .toList();
        } catch (Exception e) {
            log.warn("네이버 웹소설 검색 실패: platform={} message={}", platform.name(), e.getMessage());
            return List.of();
        }
    }

    private WebNovelSearchResult toResult(
            String query,
            WebNovelPlatform platform,
            NaverWebSearchResponse.Item item
    ) {
        WebNovelPlatform.ResolvedWork resolved = platform.resolve(item.link()).orElse(null);
        if (resolved == null) return null;

        String title = cleanTitle(item.title());
        if (!titleMatches(query, title)) return null;
        String description = cleanText(item.description());
        return new WebNovelSearchResult(
                title,
                "",
                platform.source(),
                platform.label(),
                resolved.canonicalUrl(),
                resolved.externalId(),
                truncate(description, 240)
        );
    }

    private String cleanTitle(String value) {
        return cleanText(value)
                .replaceAll("\\s*[|:：-]\\s*(네이버\\s*시리즈|카카오페이지|리디.*|웹소설\\s*문피아).*$", "")
                .replaceFirst("^(매일\\s*\\d{1,2}시\\s*무료|매일\\s*무료|기다리면\\s*무료|선독점|독점)\\s*", "")
                .replaceAll("\\s*\\[(독점|선독점|완결|무료)\\]\\s*$", "")
                .replaceAll("\\s+\\d+화$", "")
                .trim();
    }

    private String cleanText(String value) {
        if (value == null) return "";
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", ""))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean titleMatches(String query, String title) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        return !normalizedQuery.isBlank()
                && !normalizedTitle.isBlank()
                && (normalizedTitle.contains(normalizedQuery) || normalizedQuery.contains(normalizedTitle));
    }

    private String normalize(String value) {
        return value == null ? "" : value
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength).trim() + "...";
    }
}
