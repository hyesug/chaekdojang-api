package com.chaekdojang.api.domain.book;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.WebNovelRegisterRequest;
import com.chaekdojang.api.domain.book.dto.WebNovelSearchResult;
import com.chaekdojang.api.domain.review.ReviewRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.infra.kakao.KakaoWebNovelClient;
import com.chaekdojang.api.infra.naver.NaverWebNovelClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebNovelService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final KakaoWebNovelClient kakaoWebNovelClient;
    private final NaverWebNovelClient naverWebNovelClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<WebNovelSearchResult> search(String query) {
        String normalized = cleanText(query, 100);
        if (normalized.length() < 2) return List.of();

        String cacheKey = "web-novel-search:v9:" + normalized.toLowerCase(Locale.ROOT);
        List<WebNovelSearchResult> cached = readCache(cacheKey);
        if (cached != null) return cached;

        Map<String, WebNovelSearchResult> merged = new LinkedHashMap<>();
        addResults(merged, naverWebNovelClient.search(normalized));
        addResults(merged, kakaoWebNovelClient.search(normalized));
        List<WebNovelSearchResult> results = merged.values().stream()
                .sorted((left, right) -> Integer.compare(
                        titleMatchRank(normalized, left.title()),
                        titleMatchRank(normalized, right.title())
                ))
                .toList();
        writeCache(cacheKey, results);
        return results;
    }

    private int titleMatchRank(String query, String title) {
        String normalizedQuery = normalizeTitle(query);
        String normalizedTitle = normalizeTitle(title);
        return normalizedTitle.equals(normalizedQuery) ? 0 : 1;
    }

    private String normalizeTitle(String value) {
        return value == null ? "" : value
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private void addResults(Map<String, WebNovelSearchResult> target, List<WebNovelSearchResult> results) {
        for (WebNovelSearchResult result : results) {
            target.putIfAbsent(result.platform().name() + ":" + result.externalId(), result);
        }
    }

    @Transactional
    public BookResponse register(WebNovelRegisterRequest request) {
        WebNovelPlatform platform = WebNovelPlatform.fromSource(request.platform())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
        WebNovelPlatform.ResolvedWork resolved = platform.resolve(request.sourceUrl())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        Book book = bookRepository.findBySourceAndExternalId(request.platform(), resolved.externalId())
                .orElseGet(() -> createBook(request, platform, resolved));
        long reviewCount = reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId());
        return BookResponse.from(book, reviewCount);
    }

    private Book createBook(
            WebNovelRegisterRequest request,
            WebNovelPlatform platform,
            WebNovelPlatform.ResolvedWork resolved
    ) {
        String title = cleanText(request.title(), 255);
        if (title.isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST);
        String author = cleanText(request.author(), 255);
        String slugKey = request.platform().name().toLowerCase(Locale.ROOT) + "-" + resolved.externalId();
        return bookRepository.save(
                Book.builder()
                        .title(title)
                        .author(author)
                        .publisher(platform.labelFor(resolved))
                        .slug(BookSlugGenerator.create(title, author, slugKey, null))
                        .source(request.platform())
                        .externalId(resolved.externalId())
                        .sourceUrl(resolved.canonicalUrl())
                        .category("웹소설")
                        .build()
        );
    }

    private String cleanText(String value, int maxLength) {
        if (value == null) return "";
        String cleaned = HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", ""))
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }

    private List<WebNovelSearchResult> readCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) return null;
            return objectMapper.readValue(value, new TypeReference<List<WebNovelSearchResult>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCache(String key, List<WebNovelSearchResult> results) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(results), Duration.ofMinutes(30));
        } catch (Exception ignored) {
            // Redis 장애가 검색 자체를 막지 않도록 무시한다.
        }
    }
}
