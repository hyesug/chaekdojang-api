package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.domain.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FortuneProfileFlowTest {

    private static final String BODY = """
            {"name":"홍길동","gender":"female","year":1992,"month":1,"day":30,
             "hour":16,"minute":28,"birthPlace":"여주","homePlace":"서울","dst":false}
            """;

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired FortuneProfileRepository profiles;
    @Autowired UserService userService;

    Long userId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userId = users.save(User.create("fortune-" + suffix + "@test.com", "운세" + suffix, null)).getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        profiles.findByUserId(userId).ifPresent(profiles::delete);
    }

    /** 실제 JWT 필터처럼 principal 에 사용자 id(Long)를 넣는다 */
    private UsernamePasswordAuthenticationToken token() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor me() {
        return authentication(token());
    }

    @Test
    void anonymousCannotReadOrSave() throws Exception {
        mvc.perform(get("/api/fortune-profiles/me").with(anonymous()))
                .andExpect(status().is4xxClientError());
        mvc.perform(put("/api/fortune-profiles/me").with(anonymous())
                        .contentType("application/json").content(BODY))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void saveReadUpdateAndDelete() throws Exception {
        mvc.perform(get("/api/fortune-profiles/me").with(me()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json").content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("홍길동"));

        // 시각을 모르면 분도 같이 비운다
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json")
                        .content(BODY.replace("\"hour\":16", "\"hour\":null")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/fortune-profiles/me").with(me()))
                .andExpect(jsonPath("$.data.year").value(1992))
                .andExpect(jsonPath("$.data.hour").doesNotExist())
                .andExpect(jsonPath("$.data.minute").doesNotExist());
        assertThat(profiles.count()).isGreaterThanOrEqualTo(1);

        mvc.perform(delete("/api/fortune-profiles/me").with(me()))
                .andExpect(status().isOk());
        assertThat(profiles.findByUserId(userId)).isEmpty();
    }

    @Test
    void rejectsImpossibleDate() throws Exception {
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json")
                        .content(BODY.replace("\"month\":1,\"day\":30", "\"month\":2,\"day\":30")))
                .andExpect(status().isBadRequest());
        assertThat(profiles.findByUserId(userId)).isEmpty();
    }

    @Test
    void accountDeletionRemovesFortuneProfile() throws Exception {
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json").content(BODY))
                .andExpect(status().isOk());

        SecurityContextHolder.getContext().setAuthentication(token());
        userService.deleteMe();

        assertThat(profiles.findByUserId(userId)).isEmpty();
    }
}
