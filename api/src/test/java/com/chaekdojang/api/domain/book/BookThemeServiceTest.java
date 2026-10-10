package com.chaekdojang.api.domain.book;

import com.chaekdojang.api.domain.book.dto.BookThemeRecommendationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookThemeServiceTest {

    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired BookRepository books;
    @Autowired BookThemeService service;
    @Autowired OpenAiBookThemeClient client;

    private final List<Long> created = new ArrayList<>();

    @AfterEach
    void cleanup() {
        tx.executeWithoutResult(s -> created.forEach(id -> books.findById(id).ifPresent(books::delete)));
        created.clear();
    }

    private Book book(String title, String category, String description, List<String> themes) {
        return tx.execute(s -> {
            Book b = books.save(Book.builder()
                    .title(title).author("저자").source(BookSource.KAKAO)
                    .externalId(UUID.randomUUID().toString()).category(category).description(description)
                    .build());
            if (themes != null) b.applyThemes(themes);
            created.add(b.getId());
            return b;
        });
    }

    @Test
    void 주제마다_책을_고르고_주제끼리_같은_책을_겹치지_않는다() {
        Book a = book("쉼의 기술", "자기계발", "지친 마음을 쉬게 하는 법", List.of("REST", "MIND"));
        Book b = book("불안 다루기", "인문", "불안과 걱정을 다루는 법", List.of("MIND"));
        book("수능 특강", "중/고등참고서", "수능 대비 문제집", List.of("REST"));

        List<BookThemeRecommendationResponse> out = service.recommend(List.of("REST", "MIND", "NOPE"), 2);

        assertThat(out).extracting(BookThemeRecommendationResponse::theme).containsExactly("REST", "MIND");
        // 학습서는 추천하지 않는다
        assertThat(out.get(0).books()).extracting(x -> x.id()).containsExactly(a.getId());
        // 쉼에서 이미 고른 책은 마음·감정에서 다시 고르지 않는다
        assertThat(out.get(1).books()).extracting(x -> x.id()).containsExactly(b.getId());
    }

    @Test
    void 태그를_붙여_보지_않은_소개글_있는_책만_보낸다() {
        Book target = book("결정의 기술", "경제/경영", "더 나은 결정을 내리는 법을 다룬 책입니다. 결정 피로를 줄이고 우선순위를 세우는 방법을 차근차근 알려 줍니다.", null);
        book("이미 붙인 책", "인문", "이미 태그를 붙인 책입니다. 소개글이 충분히 깁니다. 충분히.", List.of());
        book("어린이 책", "어린이(초등)", "어린이를 위한 그림책입니다. 소개글이 충분히 깁니다. 충분히.", null);

        List<Book> pending = tx.execute(s -> books.findUntaggedForThemes(BookTheme.EXCLUDED_CATEGORIES,
                org.springframework.data.domain.PageRequest.of(0, 50)));

        assertThat(pending).extracting(Book::getId).contains(target.getId());
        assertThat(pending).extracting(Book::getTitle).doesNotContain("이미 붙인 책", "어린이 책");
    }

    @Test
    void AI_응답에서_목록_밖_주제와_넷째_주제는_버린다() {
        Map<Long, List<String>> parsed = client.parse("""
                {"items":[{"id":7,"themes":["rest","MONEY","UNKNOWN","MIND","HABIT"]},{"id":8,"themes":[]}]}
                """);
        assertThat(parsed.get(7L)).containsExactly("REST", "MONEY", "MIND");
        assertThat(parsed.get(8L)).isEmpty();
    }

    @Test
    void 추천_API는_로그인_없이_부를_수_있다() throws Exception {
        book("돈의 감각", "경제/경영", "돈을 대하는 태도", List.of("MONEY"));
        mvc.perform(get("/api/books/recommend").param("themes", "MONEY").param("perTheme", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].theme").value("MONEY"))
                .andExpect(jsonPath("$.data[0].books[0].title").value("돈의 감각"));
    }
}
