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
import com.chaekdojang.api.infra.webnovel.WebNovelMetadataClient;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebNovelService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final KakaoWebNovelClient kakaoWebNovelClient;
    private final NaverWebNovelClient naverWebNovelClient;
    private final WebNovelMetadataClient webNovelMetadataClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<WebNovelSearchResult> search(String query) {
        String normalized = cleanText(query, 100);
        if (normalized.length() < 2) return List.of();

        String cacheKey = "web-novel-search:v12:" + normalized.toLowerCase(Locale.ROOT);
        List<WebNovelSearchResult> cached = readCache(cacheKey);
        if (cached != null) return cached;

        Map<String, WebNovelSearchResult> merged = new LinkedHashMap<>();
        addResults(merged, naverWebNovelClient.search(normalized));
        addResults(merged, kakaoWebNovelClient.search(normalized));
        List<WebNovelSearchResult> enriched = enrichMetadata(List.copyOf(merged.values()));
        List<WebNovelSearchResult> results = deduplicateRidiWorks(enriched).stream()
                .sorted((left, right) -> Integer.compare(
                        titleMatchRank(normalized, left.title()),
                        titleMatchRank(normalized, right.title())
                ))
                .toList();
        writeCache(cacheKey, results);
        return results;
    }

    private List<WebNovelSearchResult> deduplicateRidiWorks(List<WebNovelSearchResult> results) {
        Map<String, WebNovelSearchResult> deduplicated = new LinkedHashMap<>();
        for (WebNovelSearchResult result : results) {
            String key = result.platform() == BookSource.RIDI
                    ? "RIDI:" + normalizeTitle(result.title()) + ":" + normalizeTitle(result.author())
                    : result.platform().name() + ":" + result.externalId();
            deduplicated.putIfAbsent(key, result);
        }
        return List.copyOf(deduplicated.values());
    }

    private List<WebNovelSearchResult> enrichMetadata(List<WebNovelSearchResult> results) {
        if (results.isEmpty()) return results;
        int enrichmentCount = Math.min(results.size(), 12);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, enrichmentCount));
        try {
            List<Callable<WebNovelSearchResult>> tasks = results.subList(0, enrichmentCount).stream()
                    .<Callable<WebNovelSearchResult>>map(result -> () -> enrichMetadata(result))
                    .toList();
            List<Future<WebNovelSearchResult>> futures = executor.invokeAll(tasks, 12, TimeUnit.SECONDS);
            List<WebNovelSearchResult> enriched = new java.util.ArrayList<>(results.size());
            for (int index = 0; index < futures.size(); index++) {
                Future<WebNovelSearchResult> future = futures.get(index);
                try {
                    enriched.add(future.isCancelled() ? results.get(index) : future.get());
                } catch (ExecutionException e) {
                    enriched.add(results.get(index));
                }
            }
            enriched.addAll(results.subList(enrichmentCount, results.size()));
            return List.copyOf(enriched);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private WebNovelSearchResult enrichMetadata(WebNovelSearchResult result) {
        WebNovelMetadataClient.Metadata metadata = webNovelMetadataClient.findMetadata(
                result.platform(), result.sourceUrl());
        String author = result.author().isBlank() ? metadata.author() : result.author();
        String thumbnail = metadata.thumbnail().isBlank() ? result.thumbnail() : metadata.thumbnail();
        if (author.equals(result.author()) && java.util.Objects.equals(thumbnail, result.thumbnail())) {
            return result;
        }
        return new WebNovelSearchResult(
                result.title(),
                author,
                result.platform(),
                result.platformLabel(),
                result.sourceUrl(),
                result.externalId(),
                result.description(),
                thumbnail
        );
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
                .orElse(null);
        if (book == null) {
            WebNovelMetadataClient.Metadata metadata = webNovelMetadataClient.findMetadata(
                    request.platform(), resolved.canonicalUrl());
            book = createBook(request, platform, resolved, metadata.thumbnail());
        } else if (book.getThumbnail() == null || book.getThumbnail().isBlank()) {
            WebNovelMetadataClient.Metadata metadata = webNovelMetadataClient.findMetadata(
                    request.platform(), resolved.canonicalUrl());
            book.updateThumbnailIfMissing(metadata.thumbnail());
        }
        long reviewCount = reviewRepository.countByBookIdAndDeletedAtIsNullAndHiddenFalse(book.getId());
        return BookResponse.from(book, reviewCount);
    }

    private Book createBook(
            WebNovelRegisterRequest request,
            WebNovelPlatform platform,
            WebNovelPlatform.ResolvedWork resolved,
            String thumbnail
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
                        .thumbnail(thumbnail)
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
