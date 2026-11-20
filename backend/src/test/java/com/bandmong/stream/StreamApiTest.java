package com.bandmong.stream;

import com.bandmong.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StreamApiTest extends ApiTestSupport {

    @Autowired
    SseHub hub;

    @Test
    void 로그인하지_않으면_연결할_수_없다() throws Exception {
        mvc.perform(get("/api/stream")).andExpect(status().isUnauthorized());
    }

    @Test
    void 새_메시지와_알림이_열린_연결로_온다() throws Exception {
        Person host = person("실시간방장", "[\"GUITAR\"]");
        Person bass = person("실시간베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "실시간 밴드");

        MvcResult stream = mvc.perform(get("/api/stream").session(host.session()))
                .andExpect(request().asyncStarted()).andReturn();
        assertThat(hub.connections(host.id())).isEqualTo(1);

        expect(post_(bass, "/api/bands/" + bandId + "/messages", "{\"body\":\"실시간 확인\"}"), 200);
        expect(delete_(bass, "/api/bands/" + bandId + "/members/me"), 204);

        String body = stream.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains("event:ready");
        assertThat(body).contains("event:message").contains("실시간 확인");
        assertThat(body).contains("event:notification").contains("MEMBER_LEFT");
    }

    @Test
    void 실패한_요청은_아무것도_보내지_않는다() throws Exception {
        Person host = person("롤백방장", "[\"GUITAR\"]");
        Person bass = person("롤백베이스", "[\"BASS\"]");
        Person bass2 = person("롤백베이스둘", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "롤백 밴드");

        // 베이스 자리를 1개 더 열고 두 번째 베이스가 지원 → 자리를 다시 줄이면 수락이 409로 실패
        expect(patch_(host, "/api/bands/" + bandId, "{\"seats\":{\"GUITAR\":1,\"BASS\":2,\"DRUM\":1}}"), 200);
        expect(post_(bass2, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"BASS\"}"), 200);
        long appId = read(get_(host, "/api/bands/" + bandId)).get("applications").get(0).get("id").asLong();
        expect(patch_(host, "/api/bands/" + bandId, "{\"seats\":{\"GUITAR\":1,\"BASS\":1,\"DRUM\":1}}"), 200);

        MvcResult stream = mvc.perform(get("/api/stream").session(bass2.session()))
                .andExpect(request().asyncStarted()).andReturn();
        expect(post_(host, "/api/bands/" + bandId + "/applications/" + appId + "/accept", null), 409);

        String body = stream.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).contains("event:ready");
        assertThat(body).doesNotContain("event:notification");
        assertThat(read(get_(bass2, "/api/notifications")).get("items")).isEmpty();
    }
}
