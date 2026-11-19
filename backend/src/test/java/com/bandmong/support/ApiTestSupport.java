package com.bandmong.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 여러 사람이 번갈아 요청하는 시나리오를 짧게 쓰기 위한 도우미 */
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** 로그인된 사람 한 명 */
    public record Person(MockHttpSession session, long id) {
    }

    /** 개발용 로그인 + 프로필 설정까지 마친 사람 */
    protected Person person(String nickname, String instrumentsJson) throws Exception {
        MockHttpSession s = (MockHttpSession) mvc.perform(post("/api/dev/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + UUID.randomUUID().toString().substring(0, 12) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        String body = "{\"nickname\":\"" + nickname + "\",\"instruments\":" + instrumentsJson + ",\"level\":\"HOBBY\"}";
        JsonNode me = read(mvc.perform(put("/api/me/profile").session(s).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()));
        return new Person(s, me.get("id").asLong());
    }

    protected ResultActions get_(Person p, String url) throws Exception {
        return mvc.perform(get(url).session(p.session()));
    }

    protected ResultActions post_(Person p, String url, String body) throws Exception {
        var req = post(url).session(p.session()).with(csrf());
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req);
    }

    protected ResultActions patch_(Person p, String url, String body) throws Exception {
        return mvc.perform(patch(url).session(p.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions delete_(Person p, String url) throws Exception {
        return mvc.perform(delete(url).session(p.session()).with(csrf()));
    }

    /** 기대한 상태 코드가 아니면 응답 본문을 실패 메시지에 담는다 (CI 로그에서 원인을 바로 보이게) */
    protected ResultActions expect(ResultActions r, int status) throws Exception {
        var res = r.andReturn().getResponse();
        org.assertj.core.api.Assertions.assertThat(res.getStatus())
                .as("HTTP %s body=%s", res.getStatus(), res.getContentAsString())
                .isEqualTo(status);
        return r;
    }

    protected JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }
}
