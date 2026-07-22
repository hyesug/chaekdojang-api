package com.chaekdojang.api.infra.naver;

import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class NaverWebNovelClient {

    private static final String NAVER_WEB_NOVEL_LIST_PATH = "^/(webnovel|best|challenge)/list(?:\\.(?:nhn|series))?$";
    private static final Pattern NAVER_WEB_NOVEL_ITEM = Pattern.compile(
            "<li[^>]+class=\"[^\"]*\\bitem\\b[^\"]*\"[^>]*>([\\s\\S]*?)</li>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_WEB_NOVEL_URL = Pattern.compile(
            "href=\"([^\"]*/(?:webnovel|best|challenge)/list(?:\\.(?:nhn|series))?\\?[^\"]*novelId=\\d+[^\"]*)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_WEB_NOVEL_TITLE = Pattern.compile(
            "class=\"title\"[^>]*>[\\s\\S]*?<strong>([\\s\\S]*?)</strong>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_WEB_NOVEL_AUTHOR = Pattern.compile(
            "class=\"author\"[^>]*>([\\s\\S]*?)</span>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_WEB_NOVEL_TAG = Pattern.compile(
            "class=\"tag\"[^>]*>([\\s\\S]*?)</span>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MENTIONED_WORK_TITLE = Pattern.compile("^[<《「『](.+?)[>》」』]");

    private final RestClient restClient;
    private final RestClient naverWebNovelRestClient;
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
        this.naverWebNovelRestClient = RestClient.builder()
                .baseUrl("https://novel.naver.com")
                .build();
    }

    public List<WebNovelSearchResult> search(String query) {
        Map<String, WebNovelSearchResult> results = new LinkedHashMap<>();
        List<WebNovelSearchResult> officialResults = new ArrayList<>(searchNaverWebNovelPage(query));
        if (officialResults.isEmpty() && configured) {
            for (String titleHint : findNaverWebNovelTitleHints(query)) {
                officialResults.addAll(searchNaverWebNovelPage(titleHint));
            }
        }
        for (WebNovelSearchResult result : officialResults) {
            results.putIfAbsent(result.platform().name() + ":" + result.externalId(), result);
        }
        if (!configured) return new ArrayList<>(results.values());

        for (WebNovelPlatform platform : WebNovelPlatform.values()) {
            for (WebNovelSearchResult result : searchPlatform(query, platform)) {
                results.putIfAbsent(result.platform().name() + ":" + result.externalId(), result);
            }
        }
        return new ArrayList<>(results.values());
    }

    private List<String> findNaverWebNovelTitleHints(String query) {
        try {
            NaverWebSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/v1/search/webkr.json")
                            .queryParam("query", query + " site:novel.naver.com")
                            .queryParam("display", 100)
                            .build())
                    .retrieve()
                    .body(NaverWebSearchResponse.class);
            if (response == null || response.items() == null) return List.of();

            return response.items().stream()
                    .map(item -> extractMentionedWorkTitle(query, item.title()))
                    .filter(title -> !title.isBlank())
                    .distinct()
                    .limit(3)
                    .toList();
        } catch (Exception e) {
            log.warn("네이버 웹소설 제목 보정 실패: query={} message={}", query, e.getMessage());
            return List.of();
        }
    }

    String extractMentionedWorkTitle(String query, String rawTitle) {
        String title = cleanText(rawTitle);
        Matcher matcher = MENTIONED_WORK_TITLE.matcher(title);
        if (!matcher.find()) return "";

        String mentionedTitle = matcher.group(1).trim();
        return titleMatches(query, mentionedTitle) ? mentionedTitle : "";
    }

    private List<WebNovelSearchResult> searchNaverWebNovelPage(String query) {
        try {
            String html = naverWebNovelRestClient.get()
                    .uri(builder -> builder
                            .path("/search")
                            .queryParam("keyword", query)
                            .build())
                    .retrieve()
                    .body(String.class);
            return parseNaverWebNovelSearch(query, html);
        } catch (Exception e) {
            log.warn("네이버 웹소설 공식 검색 실패: query={} message={}", query, e.getMessage());
            return List.of();
        }
    }

    List<WebNovelSearchResult> parseNaverWebNovelSearch(String query, String html) {
        if (html == null || html.isBlank()) return List.of();

        Map<String, WebNovelSearchResult> results = new LinkedHashMap<>();
        Matcher itemMatcher = NAVER_WEB_NOVEL_ITEM.matcher(html);
        while (itemMatcher.find()) {
            String item = itemMatcher.group(1);
            String rawUrl = firstGroup(NAVER_WEB_NOVEL_URL, item);
            String title = cleanText(firstGroup(NAVER_WEB_NOVEL_TITLE, item));
            if (rawUrl.isBlank() || !titleMatches(query, title)) continue;

            String sourceUrl = URI.create("https://novel.naver.com")
                    .resolve(HtmlUtils.htmlUnescape(rawUrl))
                    .toString();
            WebNovelPlatform.ResolvedWork resolved = WebNovelPlatform.NAVER_SERIES
                    .resolve(sourceUrl)
                    .orElse(null);
            if (resolved == null) continue;

            WebNovelSearchResult result = new WebNovelSearchResult(
                    title,
                    cleanText(firstGroup(NAVER_WEB_NOVEL_AUTHOR, item)),
                    WebNovelPlatform.NAVER_SERIES.source(),
                    WebNovelPlatform.NAVER_SERIES.labelFor(resolved),
                    resolved.canonicalUrl(),
                    resolved.externalId(),
                    truncate(cleanText(firstGroup(NAVER_WEB_NOVEL_TAG, item)), 240)
            );
            results.putIfAbsent(result.externalId(), result);
        }
        return new ArrayList<>(results.values());
    }

    private String firstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<WebNovelSearchResult> searchPlatform(String query, WebNovelPlatform platform) {
        Map<String, WebNovelSearchResult> results = new LinkedHashMap<>();
        for (String searchQuery : searchQueries(query, platform)) {
            for (WebNovelSearchResult result : requestSearch(query, platform, searchQuery)) {
                results.putIfAbsent(result.platform().name() + ":" + result.externalId(), result);
            }
            if (platform == WebNovelPlatform.NAVER_SERIES
                    && results.values().stream().anyMatch(result -> isExactTitle(query, result.title()))) {
                break;
            }
        }
        return results.values().stream().limit(5).toList();
    }

    private List<WebNovelSearchResult> requestSearch(
            String query,
            WebNovelPlatform platform,
            String searchQuery
    ) {
        try {
            NaverWebSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/v1/search/webkr.json")
                            .queryParam("query", searchQuery)
                            .queryParam("display", platform == WebNovelPlatform.NAVER_SERIES ? 100 : 20)
                            .build())
                    .retrieve()
                    .body(NaverWebSearchResponse.class);
            if (response == null || response.items() == null) return List.of();

            return response.items().stream()
                    .map(item -> toResult(query, platform, item))
                    .filter(result -> result != null)
                    .toList();
        } catch (Exception e) {
            log.warn("네이버 웹소설 검색 실패: platform={} query={} message={}",
                    platform.name(), searchQuery, e.getMessage());
            return List.of();
        }
    }

    List<String> searchQueries(String query, WebNovelPlatform platform) {
        Set<String> queries = new LinkedHashSet<>();
        queries.add(platform.searchQuery(query));
        if (platform == WebNovelPlatform.NAVER_SERIES) {
            queries.add("\"" + query + "\" site:series.naver.com/novel");
            queries.add(query + " 네이버 시리즈 site:series.naver.com/novel");
            queries.add(query + " site:novel.naver.com");
            queries.add("\"" + query + "\" site:novel.naver.com");
        }
        return new ArrayList<>(queries);
    }

    private WebNovelSearchResult toResult(
            String query,
            WebNovelPlatform platform,
            NaverWebSearchResponse.Item item
    ) {
        if (!isSearchResultUrl(platform, item.link())) return null;
        WebNovelPlatform.ResolvedWork resolved = platform.resolve(item.link()).orElse(null);
        if (resolved == null) return null;

        String title = cleanTitle(item.title());
        if (!titleMatches(query, title)) return null;
        String description = cleanText(item.description());
        return new WebNovelSearchResult(
                title,
                "",
                platform.source(),
                platform.labelFor(resolved),
                resolved.canonicalUrl(),
                resolved.externalId(),
                truncate(description, 240)
        );
    }

    boolean isSearchResultUrl(WebNovelPlatform platform, String rawUrl) {
        if (platform != WebNovelPlatform.NAVER_SERIES) return true;
        try {
            URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
            String host = uri.getHost();
            if (!("novel.naver.com".equalsIgnoreCase(host) || "m.novel.naver.com".equalsIgnoreCase(host))) {
                return true;
            }
            return uri.getPath() != null && uri.getPath().matches(NAVER_WEB_NOVEL_LIST_PATH);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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
                && normalizedTitle.startsWith(normalizedQuery);
    }

    private boolean isExactTitle(String query, String title) {
        return normalize(query).equals(normalize(title));
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
