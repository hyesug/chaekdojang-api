package com.chaekdojang.api.infra.ridi;

import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RidiWebNovelClient {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ChaekDojang/1.0)";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public RidiWebNovelClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl("https://ridibooks.com")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    public List<WebNovelSearchResult> search(String query) {
        try {
            String html = restClient.get()
                    .uri(builder -> builder.path("/search").queryParam("q", query).build())
                    .retrieve()
                    .body(String.class);
            return parseSearchResults(query, html);
        } catch (Exception e) {
            log.warn("리디 공식 웹소설 검색 실패: query={} message={}", query, e.getMessage());
            return List.of();
        }
    }

    List<WebNovelSearchResult> parseSearchResults(String query, String html) {
        if (html == null || html.isBlank()) return List.of();
        Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
        if (!matcher.find()) return List.of();

        try {
            JsonNode root = objectMapper.readTree(matcher.group(1));
            JsonNode cells = root.path("props").path("pageProps").path("gridData")
                    .path("riGrid").path("grid").path("cells");
            if (!cells.isArray()) return List.of();

            Map<String, WebNovelSearchResult> results = new LinkedHashMap<>();
            for (JsonNode cell : cells) {
                JsonNode books = cell.path("cell__SearchBookListWithTab").path("books");
                if (!books.isArray()) continue;
                for (JsonNode entry : books) {
                    WebNovelSearchResult result = toResult(query, entry.path("book"));
                    if (result != null) results.putIfAbsent(result.externalId(), result);
                }
            }
            return new ArrayList<>(results.values());
        } catch (Exception e) {
            log.warn("리디 공식 검색 결과 파싱 실패: query={} message={}", query, e.getMessage());
            return List.of();
        }
    }

    private WebNovelSearchResult toResult(String query, JsonNode book) {
        JsonNode series = book.path("series");
        String externalId = cleanText(series.path("id").asText(""));
        String title = cleanText(series.path("title").asText(""));
        if (externalId.isBlank() || title.isBlank() || !titleMatches(query, title)) return null;

        String sourceUrl = "https://ridibooks.com/books/" + externalId;
        WebNovelPlatform.ResolvedWork resolved = WebNovelPlatform.RIDI.resolve(sourceUrl).orElse(null);
        if (resolved == null) return null;

        List<String> authors = new ArrayList<>();
        for (JsonNode author : book.path("authors")) {
            String name = cleanText(author.path("name").asText(""));
            if (!name.isBlank() && !authors.contains(name)) authors.add(name);
        }

        return new WebNovelSearchResult(
                title,
                String.join(", ", authors),
                BookSource.RIDI,
                WebNovelPlatform.RIDI.labelFor(resolved),
                resolved.canonicalUrl(),
                resolved.externalId(),
                truncate(cleanText(book.path("introduction").path("description").asText("")), 240),
                sanitizeThumbnail(series.path("thumbnail").path("large").asText(""))
        );
    }

    private boolean titleMatches(String query, String title) {
        String normalizedQuery = normalize(query);
        String normalizedTitle = normalize(title);
        return !normalizedQuery.isBlank()
                && !normalizedTitle.isBlank()
                && normalizedTitle.contains(normalizedQuery);
    }

    private String sanitizeThumbnail(String value) {
        try {
            String normalized = value == null ? "" : value.trim();
            int fragmentIndex = normalized.indexOf('#');
            if (fragmentIndex >= 0) normalized = normalized.substring(0, fragmentIndex);
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) return "";
            host = host.toLowerCase(Locale.ROOT);
            return (host.equals("ridicdn.net") || host.endsWith(".ridicdn.net"))
                    && normalized.length() <= 255 ? normalized : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength).trim() + "...";
    }
}
