package com.chaekdojang.api.domain.lotto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LottoFutureValidationAuthorizationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void normalUserCannotReadLottoFutureValidationApi() throws Exception {
        mockMvc.perform(get("/api/admin/lotto-future-validations").with(user("2").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void normalUserCannotCreateLottoModelRevision() throws Exception {
        mockMvc.perform(post("/api/admin/lotto-future-validations/1243/model-revision")
                        .contentType("application/json").content("{\"reason\":\"model v2\"}")
                        .with(user("2").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
