package com.bandmong.auth;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 로그인_방법_목록과_CSRF_쿠키() throws Exception {
        mvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devLogin").value(true))
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void 같은_이름으로_다시_로그인하면_같은_계정() throws Exception {
        String body = "{\"name\":\"same-person\"}";
        String a = mvc.perform(post("/api/dev/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String b = mvc.perform(post("/api/dev/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(id(a)).isEqualTo(id(b));
    }

    @Test
    void 로그아웃하면_세션이_끊긴다() throws Exception {
        MockHttpSession s = (MockHttpSession) mvc.perform(post("/api/dev/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"bye\"}"))
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/me").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(s).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(s)).andExpect(status().isUnauthorized());
    }

    @Test
    void 서버시각은_로그인없이_볼수있다() throws Exception {
        mvc.perform(get("/api/time")).andExpect(status().isOk()).andExpect(jsonPath("$.now").exists());
    }

    @Test
    void 소셜로그인_회원번호_꺼내기() {
        assertThat(OAuthLoginSuccessHandler.providerId("kakao", Map.of("id", 123L))).isEqualTo("123");
        assertThat(OAuthLoginSuccessHandler.providerId("naver", Map.of("response", Map.of("id", "abc")))).isEqualTo("abc");
        assertThat(OAuthLoginSuccessHandler.providerId("google", Map.of("sub", "g-1"))).isEqualTo("g-1");
    }

    private static String id(String json) {
        return json.replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
