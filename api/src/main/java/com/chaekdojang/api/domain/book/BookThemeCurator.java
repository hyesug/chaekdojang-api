package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 미리 골라 둔 스테디셀러(book-themes/steady-sellers.json, 약 120권)를 책도장 책으로 등록하고 주제를 붙인다.
 * 독후감이 적은 지금은 "독후감 많은 순"이 의미가 없어, AI 가 태그한 아무 책보다 검증된 책을 먼저 추천하려는 것.
 * 책은 기존 책 검색(카카오·구글)으로 찾아 등록한다 — 제목이 맞고 저자가 겹치는 첫 결과. 못 찾으면 건너뛴다.
 * AI 를 부르지 않는다. 한 번 돌 때 몇 권씩만 찾고, 이미 등록한 책은 다시 찾지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookThemeCurator {

    public record Entry(String title, String author, List<String> themes) {}

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final BookThemeProperties properties;

    private List<Entry> entries;
    private final Set<String> attempted = new HashSet<>();

    List<Entry> entries() {
        if (entries == null) {
            try (InputStream in = new ClassPathResource("book-themes/steady-sellers.json").getInputStream()) {
                entries = objectMapper.readValue(in, new TypeReference<List<Entry>>() {});
            } catch (Exception e) {
                log.warn("book-theme curated list load failed: {}", e.getMessage());
                entries = List.of();
            }
        }
        return entries;
    }

    /** @return 이번에 등록한 책 수 */
    public int tick(int max) {
        if (!properties.isEnabled()) return 0;
        int done = 0, tried = 0;
        for (Entry e : entries()) {
            if (tried >= max) break;
            String key = norm(e.title());
            if (!attempted.add(key)) continue;
            if (bookRepository.existsCuratedByTitle(key)) continue;
            tried++;
            if (curate(e)) done++;
        }
        return done;
    }

    public boolean finished() {
        return attempted.size() >= entries().size();
    }

    boolean curate(Entry e) {
        List<BookResponse> found;
        try {
            found = bookService.search(e.title(), "", "");
        } catch (Exception ex) {
            log.warn("book-theme curate search failed: {} — {}", e.title(), ex.getMessage());
            attempted.remove(norm(e.title()));   // 검색 장애면 다음에 다시
            return false;
        }
        String t = norm(e.title());
        String surname = norm(e.author()).length() >= 2 ? norm(e.author()).substring(0, 2) : norm(e.author());
        BookResponse pick = found.stream()
                .filter(b -> norm(b.title()).startsWith(t) || norm(b.title()).contains(t))
                .filter(b -> b.author() != null && norm(b.author()).contains(surname))
                .findFirst().orElse(null);
        if (pick == null) {
            log.info("book-theme curate: not found — {} / {}", e.title(), e.author());
            return false;
        }
        List<String> codes = e.themes().stream().map(BookTheme::fromCode).flatMap(java.util.Optional::stream).map(Enum::name).toList();
        return Boolean.TRUE.equals(tx.execute(s -> bookRepository.findById(pick.id()).map(b -> {
            b.addCuratedThemes(codes);
            return true;
        }).orElse(false)));
    }

    static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[\\s,.:·!?'\"()\\-]", "");
    }
}
