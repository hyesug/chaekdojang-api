package com.chaekdojang.api.domain.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserActivityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void normalUserCannotCallAdminActivityApi() throws Exception {
        mockMvc.perform(get("/api/admin/users/2/activity")
                        .with(user("2").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
