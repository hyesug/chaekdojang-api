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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        profiles.deleteAll(profiles.findAllByUserIdOrderBySelfDescCreatedAtAscIdAsc(userId));
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
        assertThat(profiles.countByUserId(userId)).isZero();
    }

    @Test
    void savesSeveralPeopleAndKeepsMeSeparate() throws Exception {
        // 내 프로필
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json").content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.self").value(true));
        // 가족 둘 더
        String momId = com.jayway.jsonpath.JsonPath.read(mvc.perform(post("/api/fortune-profiles").with(me())
                        .contentType("application/json").content(BODY.replace("홍길동", "엄마")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.self").value(false))
                .andReturn().getResponse().getContentAsString(), "$.data.id").toString();
        mvc.perform(post("/api/fortune-profiles").with(me())
                        .contentType("application/json").content(BODY.replace("홍길동", "동생")))
                .andExpect(status().isOk());

        // 목록은 본인이 맨 앞
        mvc.perform(get("/api/fortune-profiles").with(me()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data[0].self").value(true))
                .andExpect(jsonPath("$.data[1].name").value("엄마"));

        // 가족 프로필 고치기 — /me 는 그대로
        mvc.perform(put("/api/fortune-profiles/" + momId).with(me())
                        .contentType("application/json").content(BODY.replace("홍길동", "어머니")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("어머니"));
        mvc.perform(get("/api/fortune-profiles/me").with(me()))
                .andExpect(jsonPath("$.data.name").value("홍길동"));

        // 남의 프로필은 없는 것으로 답한다
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long otherId = users.save(User.create("other-" + suffix + "@test.com", "남" + suffix, null)).getId();
        var other = authentication(new UsernamePasswordAuthenticationToken(otherId, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        mvc.perform(put("/api/fortune-profiles/" + momId).with(other)
                        .contentType("application/json").content(BODY))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/fortune-profiles/" + momId).with(other))
                .andExpect(status().isNotFound());

        // 지우기 — 내 프로필은 남는다
        mvc.perform(delete("/api/fortune-profiles/" + momId).with(me()))
                .andExpect(status().isOk());
        assertThat(profiles.countByUserId(userId)).isEqualTo(2);
        assertThat(profiles.findByUserIdAndSelfTrue(userId)).isPresent();
    }

    @Test
    void limitsToTwentyProfiles() throws Exception {
        for (int i = 0; i < FortuneProfileService.MAX_PROFILES; i++) {
            mvc.perform(post("/api/fortune-profiles").with(me())
                            .contentType("application/json").content(BODY.replace("홍길동", "사람" + i)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/fortune-profiles").with(me())
                        .contentType("application/json").content(BODY))
                .andExpect(status().isBadRequest());
        // 처음 저장한 것이 본인 프로필이 된다
        mvc.perform(get("/api/fortune-profiles/me").with(me()))
                .andExpect(jsonPath("$.data.name").value("사람0"));
    }

    @Test
    void rejectsImpossibleDate() throws Exception {
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json")
                        .content(BODY.replace("\"month\":1,\"day\":30", "\"month\":2,\"day\":30")))
                .andExpect(status().isBadRequest());
        assertThat(profiles.countByUserId(userId)).isZero();
    }

    @Test
    void accountDeletionRemovesFortuneProfile() throws Exception {
        mvc.perform(put("/api/fortune-profiles/me").with(me())
                        .contentType("application/json").content(BODY))
                .andExpect(status().isOk());

        SecurityContextHolder.getContext().setAuthentication(token());
        userService.deleteMe();

        assertThat(profiles.countByUserId(userId)).isZero();
    }
}
