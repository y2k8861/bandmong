package com.bandmong.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MeApiTest {

    @Autowired
    MockMvc mvc;

    private MockHttpSession login(String name) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/dev/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void 로그인_없이_내정보를_보면_401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 처음_로그인하면_온보딩_전_상태() throws Exception {
        MockHttpSession s = login("me-first");
        mvc.perform(get("/api/me").session(s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("dev"))
                .andExpect(jsonPath("$.onboarded").value(false));
    }

    @Test
    void 프로필을_저장하면_온보딩_완료() throws Exception {
        MockHttpSession s = login("me-profile");
        mvc.perform(put("/api/me/profile").session(s).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"퇴근베이스","instruments":["BASS","VOCAL"],"level":"HOBBY","region":"마포구"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(true))
                .andExpect(jsonPath("$.instruments", contains("VOCAL", "BASS")));

        mvc.perform(get("/api/me").session(s))
                .andExpect(jsonPath("$.nickname").value("퇴근베이스"))
                .andExpect(jsonPath("$.region").value("마포구"));
    }

    @Test
    void 닉네임_규칙에_안맞으면_400과_안내문구() throws Exception {
        MockHttpSession s = login("me-bad");
        mvc.perform(put("/api/me/profile").session(s).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"a\",\"instruments\":[\"DRUM\"],\"level\":\"BEGINNER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("닉네임은 한글·영문·숫자 2~10자예요"));
    }

    @Test
    void 악기를_안고르면_400() throws Exception {
        MockHttpSession s = login("me-noinst");
        mvc.perform(put("/api/me/profile").session(s).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"드러머\",\"instruments\":[],\"level\":\"STAGE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("악기를 하나 이상 골라 주세요"));
    }

    @Test
    void CSRF_토큰_없는_쓰기요청은_403() throws Exception {
        MockHttpSession s = login("me-csrf");
        mvc.perform(put("/api/me/profile").session(s)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"테스트\",\"instruments\":[\"KEYS\"],\"level\":\"HOBBY\"}"))
                .andExpect(status().isForbidden());
    }
}
