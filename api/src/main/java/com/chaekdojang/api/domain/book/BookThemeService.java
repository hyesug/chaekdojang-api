package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.chaekdojang.api.domain.book.dto.BookThemeRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 책 주제 태그와 주제별 추천.
 *   tagPending — 태그를 붙여 보지 않은 책을 묶어 AI 에 한 번 보낸다(책당 한 번)
 *   seedThin   — 태그 책이 적은 주제 하나를 골라 검색어로 후보 책을 모은다(검색 한 번)
 *   recommend  — 주제마다 독후감이 많은 책부터, 주제끼리 겹치지 않게
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookThemeService {

    private final BookRepository bookRepository;
    private final OpenAiBookThemeClient client;
    private final BookThemeProperties properties;
    private final BookService bookService;

    private int seedCursor = 0;

    @Transactional
    public int tagPending() {
        if (!properties.isEnabled() || !client.isConfigured()) return 0;
        List<Book> books = bookRepository.findUntaggedForThemes(BookTheme.EXCLUDED_CATEGORIES,
                PageRequest.of(0, Math.max(1, properties.getBatchSize())));
        if (books.isEmpty()) return 0;
        List<OpenAiBookThemeClient.Input> inputs = books.stream()
                .map(b -> new OpenAiBookThemeClient.Input(b.getId(), b.getTitle(), BookGenreClassifier.resolve(b),
                        clip(b.getDescription(), properties.getDescriptionChars())))
                .toList();
        Map<Long, List<String>> tags = client.tag(inputs);
        for (Book b : books) b.applyThemes(tags.getOrDefault(b.getId(), List.of()));
        return books.size();
    }

    /** 태그 책이 가장 적은 주제의 검색어를 하나 돌린다 — 찾은 책은 다음 tagPending 에서 태그가 붙는다 */
    public void seedThin() {
        if (!properties.isEnabled()) return;
        BookTheme thin = null;
        long least = Long.MAX_VALUE;
        for (BookTheme t : BookTheme.values()) {
            long n = bookRepository.countByTheme(t.name());
            if (n < least) { least = n; thin = t; }
        }
        if (thin == null || least >= properties.getSeedMinBooks()) return;
        String query = thin.seeds().get(seedCursor++ % thin.seeds().size());
        try {
            bookService.search(query, "", "");
        } catch (Exception e) {
            log.warn("book-theme seed failed: theme={} query={} error={}", thin, query, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<BookThemeRecommendationResponse> recommend(List<String> codes, int perTheme) {
        int limit = Math.max(1, Math.min(perTheme, 5));
        Set<Long> used = new HashSet<>();
        List<BookThemeRecommendationResponse> out = new ArrayList<>();
        for (String code : codes) {
            BookTheme theme = BookTheme.fromCode(code).orElse(null);
            if (theme == null) continue;
            List<BookResponse> books = new ArrayList<>();
            // 다른 주제에서 이미 고른 책을 건너뛰어도 모자라지 않게 넉넉히 받는다
            for (Object[] row : bookRepository.findByThemeRanked(theme.name(), BookTheme.EXCLUDED_CATEGORIES,
                    PageRequest.of(0, limit + 6))) {
                Book b = (Book) row[0];
                if (!used.add(b.getId())) continue;
                books.add(BookResponse.from(b, ((Number) row[1]).longValue()));
                if (books.size() >= limit) break;
            }
            out.add(new BookThemeRecommendationResponse(theme.name(), theme.label(), books));
        }
        return out;
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
