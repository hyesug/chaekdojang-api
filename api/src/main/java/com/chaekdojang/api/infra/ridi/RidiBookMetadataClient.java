package com.chaekdojang.api.infra.ridi;

import com.chaekdojang.api.domain.book.WebNovelPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RidiBookMetadataClient {

    private static final Pattern JSON_LD_PATTERN = Pattern.compile(
            "<script[^>]*type=\"application/ld\\+json\"[^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public RidiBookMetadataClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; ChaekDojang/1.0)")
                .build();
    }

    public String findAuthor(String sourceUrl) {
        WebNovelPlatform.ResolvedWork work = WebNovelPlatform.RIDI.resolve(sourceUrl).orElse(null);
        if (work == null) return "";

        try {
            String html = restClient.get()
                    .uri(URI.create(work.canonicalUrl()))
                    .retrieve()
                    .body(String.class);
            return parseAuthor(html);
        } catch (Exception e) {
            log.warn("리디 작가 정보 조회 실패: bookId={} message={}", work.externalId(), e.getMessage());
            return "";
        }
    }

    String parseAuthor(String html) {
        if (html == null || html.isBlank()) return "";

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
        String name = authorNode.path("name").asText("").replaceAll("\\s+", " ").trim();
        if (!name.isBlank() && !names.contains(name)) names.add(name);
    }
}
