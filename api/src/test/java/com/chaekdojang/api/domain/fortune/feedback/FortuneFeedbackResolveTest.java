package com.chaekdojang.api.domain.fortune.feedback;

import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.fortune-feedback.export-token=test-export-token")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FortuneFeedbackResolveTest {

    @Autowired MockMvc mvc;
    @Autowired FortuneFeedbackRepository repository;
    @Autowired FortuneFeedbackService service;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private Long save(String section, String verdict) {
        service.create(new CreateRequest("solo", section, verdict, "문장", null));
        return repository.findAllByOrderByIdDesc(org.springframework.data.domain.PageRequest.of(0, 1)).get(0).getId();
    }

    @Test
    void 처리한_피드백은_지우지_않고_집계와_목록에서_숨긴다() {
        Long a = save("일과 돈", "down");
        save("일과 돈", "down");
        assertThat(service.resolve(List.of(a))).isEqualTo(1);

        assertThat(repository.count()).isEqualTo(2);                       // 지우지 않는다
        var s = service.summary(10);
        assertThat(s.recent()).hasSize(1);                                  // 목록에서 숨김
        assertThat(s.sections()).singleElement().satisfies(c -> assertThat(c.down()).isEqualTo(1)); // 집계에서 뺌
        assertThat(service.summary(10, true).recent()).hasSize(2);          // "처리된 것도 보기"

        service.reopen(a);
        assertThat(service.summary(10).recent()).hasSize(2);
    }

    @Test
    void 주간_작업은_전용_토큰으로만_처리할_수_있다() throws Exception {
        Long a = save("핵심 요약", "down");
        String body = "{\"ids\":[" + a + "]}";
        mvc.perform(post("/api/internal/fortune-feedback/resolve").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/internal/fortune-feedback/resolve").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Fortune-Feedback-Token", "test-export-token"))
                .andExpect(status().isOk());
        assertThat(repository.findById(a).orElseThrow().getResolvedAt()).isNotNull();
    }

    @Test
    void 관리자_처리_주소는_비회원이_쓸_수_없다() throws Exception {
        mvc.perform(post("/api/admin/fortune-feedback/resolve").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[1]}"))
                .andExpect(status().is4xxClientError());
    }
}
