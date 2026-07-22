package com.chaekdojang.api.infra.kakao;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class KakaoWebNovelClient {

    private static final Pattern AUTHOR_PATTERN = Pattern.compile(
            "(?:글|작가|저자)\\s*[:：]?\\s*([가-힣A-Za-z0-9_·.\\-]{1,30})"
    );

    private final RestClient restClient;

    public KakaoWebNovelClient(@Value("${kakao.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }

    public List<WebNovelSearchResult> search(String query) {
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
            KakaoWebSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/v2/search/web")
                            .queryParam("query", platform.searchQuery(query))
                            .queryParam("size", 10)
                            .build())
                    .retrieve()
                    .body(KakaoWebSearchResponse.class);
            if (response == null || response.documents() == null) return List.of();

            return response.documents().stream()
                    .map(document -> toResult(query, platform, document))
                    .filter(result -> result != null)
                    .limit(5)
                    .toList();
        } catch (Exception e) {
            log.warn("카카오 웹소설 검색 실패: platform={} message={}", platform.name(), e.getMessage());
            return List.of();
        }
    }

    private WebNovelSearchResult toResult(
            String query,
            WebNovelPlatform platform,
            KakaoWebSearchResponse.Document document
    ) {
        WebNovelPlatform.ResolvedWork resolved = platform.resolve(document.url()).orElse(null);
        if (resolved == null) return null;

        String title = cleanTitle(document.title());
        if (!titleMatches(query, title)) return null;
        String contents = cleanText(document.contents());
        return new WebNovelSearchResult(
                title,
                extractAuthor(contents),
                platform.source(),
                platform.labelFor(resolved),
                resolved.canonicalUrl(),
                resolved.externalId(),
                truncate(contents, 240)
        );
    }

    private String cleanTitle(String value) {
        return cleanText(value)
                .replaceAll("\\s*[|:：-]\\s*(네이버\\s*(시리즈|웹소설)|카카오페이지|리디.*|웹소설\\s*문피아).*$", "")
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

    boolean titleMatches(String query, String title) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        return !normalizedQuery.isBlank()
                && !normalizedTitle.isBlank()
                && (normalizedTitle.contains(normalizedQuery) || normalizedQuery.startsWith(normalizedTitle));
    }

    private String normalize(String value) {
        return value == null ? "" : value
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String extractAuthor(String contents) {
        Matcher matcher = AUTHOR_PATTERN.matcher(contents);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength).trim() + "...";
    }
}
