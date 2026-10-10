package com.chaekdojang.api.domain.fortune.feedback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.fortune-feedback.export-token=test-export-token")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FortuneFeedbackExportTest {

    @Autowired MockMvc mvc;

    @Test
    void 전용_토큰이_맞을_때만_내보낸다() throws Exception {
        mvc.perform(get("/api/internal/fortune-feedback")).andExpect(status().isForbidden());
        mvc.perform(get("/api/internal/fortune-feedback").header("X-Fortune-Feedback-Token", "wrong"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/internal/fortune-feedback").header("X-Fortune-Feedback-Token", "test-export-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections").isArray());
    }
}
