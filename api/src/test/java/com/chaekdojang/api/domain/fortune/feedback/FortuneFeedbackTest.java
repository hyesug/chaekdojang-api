package com.chaekdojang.api.domain.fortune.feedback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FortuneFeedbackTest {

    @Autowired MockMvc mvc;
    @Autowired FortuneFeedbackRepository repository;
    @Autowired FortuneFeedbackService service;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private String body(String mode, String section, String verdict, String comment) {
        return """
                {"mode":"%s","section":"%s","verdict":"%s","snippet":"이직이나 업종 변경을 남보다 자주 겪습니다.","comment":%s}
                """.formatted(mode, section, verdict, comment == null ? "null" : "\"" + comment + "\"");
    }

    @Test
    void 비회원도_피드백을_남길_수_있다() throws Exception {
        mvc.perform(post("/api/fortune/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content(body("solo", "일과 돈", "down", "10대부터 미용만 했어요")))
                .andExpect(status().isOk());
        assertThat(repository.findAll()).singleElement().satisfies(f -> {
            assertThat(f.getSection()).isEqualTo("일과 돈");
            assertThat(f.getVerdict()).isEqualTo("down");
            assertThat(f.getComment()).isEqualTo("10대부터 미용만 했어요");
        });
    }

    @Test
    void 형식이_틀리면_저장하지_않는다() throws Exception {
        mvc.perform(post("/api/fortune/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content(body("solo", "일과 돈", "maybe", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/fortune/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content(body("group", "일과 돈", "up", null)))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void 한_줄_입력의_연락처와_출생일은_지운다() {
        assertThat(FortuneFeedbackService.scrub("010-1234-5678로 연락 주세요")).doesNotContain("1234").contains("[생략]");
        assertThat(FortuneFeedbackService.scrub("a.b@example.com 입니다")).doesNotContain("example");
        assertThat(FortuneFeedbackService.scrub("1992년 1월 4일생이에요")).doesNotContain("1992");
        assertThat(FortuneFeedbackService.scrub("920104 친구")).doesNotContain("920104");
        assertThat(FortuneFeedbackService.scrub("아이 둘이에요")).isEqualTo("아이 둘이에요");
    }

    @Test
    void 관리자_집계는_칸별로_좋아요와_아니에요를_센다() {
        service.create(new com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest("solo", "일과 돈", "down", null, null));
        service.create(new com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest("solo", "일과 돈", "up", null, null));
        service.create(new com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest("pair", "자녀와 함께라면", "down", null, null));
        var s = service.summary(10);
        assertThat(s.sections()).anySatisfy(c -> {
            assertThat(c.section()).isEqualTo("일과 돈");
            assertThat(c.up()).isEqualTo(1);
            assertThat(c.down()).isEqualTo(1);
        });
        assertThat(s.recent()).hasSize(3);
    }

    @Test
    void 관리자_집계는_비회원이_볼_수_없다() throws Exception {
        mvc.perform(get("/api/admin/fortune-feedback")).andExpect(status().is4xxClientError());
    }
}
