package com.chaekdojang.api.infra.webnovel;

import com.chaekdojang.api.domain.book.BookSource;
import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WebNovelMetadataClient {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ChaekDojang/1.0)";
    private static final String KAKAO_IMAGE_PREFIX =
            "https://page-images.kakaoentcdn.com/download/resource?kid=";
    private static final Pattern META_TAG_PATTERN = Pattern.compile(
            "<meta\\b[^>]*>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([\"'])(.*?)\\2",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_WEB_NOVEL_COVER_PATTERN = Pattern.compile(
            "class=\"section_area_info\"[\\s\\S]{0,3000}?class=\"thumbnail\"[\\s\\S]{0,1000}?<img[^>]+src=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAVER_SERIES_COVER_PATTERN = Pattern.compile(
            "class=\"pic_area\"[\\s\\S]{0,2500}?<img[^>]+src=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JSON_LD_PATTERN = Pattern.compile(
            "<script[^>]*type=\"application/ld\\+json\"[^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public WebNovelMetadataClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    public Metadata findMetadata(BookSource source, String sourceUrl) {
        WebNovelPlatform platform = WebNovelPlatform.fromSource(source).orElse(null);
        if (platform == null) return Metadata.empty();
        WebNovelPlatform.ResolvedWork work = platform.resolve(sourceUrl).orElse(null);
        if (work == null) return Metadata.empty();

        try {
            if (source == BookSource.KAKAO_PAGE) {
                JsonNode response = restClient.get()
                        .uri("https://bff-page.kakao.com/api/gateway/api/v1/content/overview?series_id={seriesId}",
                                work.externalId())
                        .header(HttpHeaders.ORIGIN, "https://page.kakao.com")
                        .header(HttpHeaders.REFERER, work.canonicalUrl())
                        .retrieve()
                        .body(JsonNode.class);
                return parseKakaoMetadata(response);
            }

            String html = restClient.get()
                    .uri(URI.create(work.canonicalUrl()))
                    .retrieve()
                    .body(String.class);
            return parseHtmlMetadata(source, html);
        } catch (Exception e) {
            log.warn("웹소설 메타데이터 조회 실패: platform={} externalId={} message={}",
                    source.name(), work.externalId(), e.getMessage());
            return Metadata.empty();
        }
    }

    Metadata parseHtmlMetadata(BookSource source, String html) {
        if (html == null || html.isBlank()) return Metadata.empty();

        String thumbnail = "";
        if (source == BookSource.NAVER_SERIES) {
            thumbnail = firstGroup(NAVER_WEB_NOVEL_COVER_PATTERN, html);
            if (thumbnail.isBlank()) thumbnail = firstGroup(NAVER_SERIES_COVER_PATTERN, html);
        }
        if (thumbnail.isBlank()) thumbnail = openGraphImage(html);

        String author = source == BookSource.RIDI ? ridiAuthor(html) : "";
        String description = jsonLdDescription(html);
        if (description.isBlank()) description = metaDescription(html);
        return new Metadata(author, sanitizeThumbnail(source, thumbnail), description);
    }

    Metadata parseKakaoMetadata(JsonNode response) {
        if (response == null) return Metadata.empty();
        JsonNode content = response.path("result").path("content");
        String author = cleanText(content.path("authors").asText(""));
        String thumbnailKey = cleanText(content.path("thumbnail").asText(""));
        String thumbnail = thumbnailKey.isBlank() ? "" : KAKAO_IMAGE_PREFIX + thumbnailKey;
        String description = cleanDescription(content.path("description").asText(""));
        return new Metadata(author, sanitizeThumbnail(BookSource.KAKAO_PAGE, thumbnail), description);
    }

    private String metaDescription(String html) {
        Matcher tagMatcher = META_TAG_PATTERN.matcher(html);
        while (tagMatcher.find()) {
            Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(tagMatcher.group());
            String property = "";
            String content = "";
            while (attributeMatcher.find()) {
                String name = attributeMatcher.group(1).toLowerCase(Locale.ROOT);
                String value = attributeMatcher.group(3);
                if (name.equals("property") || name.equals("name")) property = value;
                if (name.equals("content")) content = value;
            }
            if ((property.equalsIgnoreCase("description")
                    || property.equalsIgnoreCase("og:description")
                    || property.equalsIgnoreCase("twitter:description")) && !content.isBlank()) {
                return cleanDescription(content);
            }
        }
        return "";
    }

    private String openGraphImage(String html) {
        Matcher tagMatcher = META_TAG_PATTERN.matcher(html);
        while (tagMatcher.find()) {
            Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(tagMatcher.group());
            String property = "";
            String content = "";
            while (attributeMatcher.find()) {
                String name = attributeMatcher.group(1).toLowerCase(Locale.ROOT);
                String value = attributeMatcher.group(3);
                if (name.equals("property") || name.equals("name")) property = value;
                if (name.equals("content")) content = value;
            }
            if ((property.equalsIgnoreCase("og:image")
                    || property.equalsIgnoreCase("og:image:secure_url")
                    || property.equalsIgnoreCase("twitter:image")) && !content.isBlank()) {
                return content;
            }
        }
        return "";
    }

    private String ridiAuthor(String html) {
        Matcher matcher = JSON_LD_PATTERN.matcher(html);
        while (matcher.find()) {
            try {
                JsonNode book = objectMapper.readTree(matcher.group(1));
                if (!"Book".equals(book.path("@type").asText())) continue;
                return authorNames(book.path("author"));
            } catch (Exception ignored) {
                // 다른 JSON-LD 블록이 파싱되지 않아도 다음 블록을 확인한다.
            }
        }
        return "";
    }

    private String jsonLdDescription(String html) {
        Matcher matcher = JSON_LD_PATTERN.matcher(html);
        while (matcher.find()) {
            try {
                JsonNode book = objectMapper.readTree(matcher.group(1));
                if (!"Book".equals(book.path("@type").asText())) continue;
                String description = cleanDescription(book.path("description").asText(""));
                if (!description.isBlank()) return description;
            } catch (Exception ignored) {
                // 다른 JSON-LD 블록이 파싱되지 않아도 다음 블록을 확인한다.
            }
        }
        return "";
    }

    private String authorNames(JsonNode authorNode) {
        List<String> names = new ArrayList<>();
        if (authorNode.isArray()) {
            authorNode.forEach(author -> addAuthorName(names, author));
        } else {
            addAuthorName(names, authorNode);
        }
        return String.join(", ", names);
    }

    private void addAuthorName(List<String> names, JsonNode authorNode) {
        String name = cleanText(authorNode.path("name").asText(""));
        if (!name.isBlank() && !names.contains(name)) names.add(name);
    }

    private String sanitizeThumbnail(BookSource source, String rawUrl) {
        try {
            String normalized = HtmlUtils.htmlUnescape(rawUrl == null ? "" : rawUrl.trim());
            if (normalized.startsWith("//")) normalized = "https:" + normalized;
            int fragmentIndex = normalized.indexOf('#');
            if (fragmentIndex >= 0) normalized = normalized.substring(0, fragmentIndex);
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) return "";
            host = host.toLowerCase(Locale.ROOT);
            boolean allowed = switch (source) {
                case NAVER_SERIES -> hostMatches(host, "pstatic.net");
                case KAKAO_PAGE -> host.equals("page-images.kakaoentcdn.com")
                        || host.equals("dn-img-page.kakao.com");
                case RIDI -> hostMatches(host, "ridicdn.net");
                case MUNPIA -> hostMatches(host, "munpia.com");
                default -> false;
            };
            return allowed && normalized.length() <= 255 ? normalized : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private boolean hostMatches(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private String firstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String cleanText(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanDescription(String value) {
        return cleanText(value == null ? "" : value
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", " "));
    }

    public record Metadata(String author, String thumbnail, String description) {
        public Metadata {
            author = author == null ? "" : author;
            thumbnail = thumbnail == null ? "" : thumbnail;
            description = description == null ? "" : description;
        }

        public static Metadata empty() {
            return new Metadata("", "", "");
        }
    }
}
