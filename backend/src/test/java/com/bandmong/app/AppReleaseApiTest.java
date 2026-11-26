package com.bandmong.app;

import com.bandmong.support.ApiTestSupport;
import com.bandmong.web.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 7단계: 앱 출시 준비 — 차단, 욕설 필터, 앱 토큰 로그인, Apple 로그인, 푸시 기기, CORS */
@SpringBootTest
@AutoConfigureMockMvc
class AppReleaseApiTest extends ApiTestSupport {

    /** 진짜 Apple 서버 대신: "valid:<sub>" 이면 통과 */
    @TestConfiguration
    static class FakeApple {
        @Bean
        @Primary
        AppleTokenVerifier fakeAppleVerifier() {
            return token -> {
                if (!token.startsWith("valid:")) throw new ApiException(HttpStatus.UNAUTHORIZED, "Apple 로그인을 확인하지 못했어요");
                return new AppleTokenVerifier.AppleIdentity(token.substring(6), "relay@privaterelay.appleid.com");
            };
        }
    }

    @Autowired
    AppAuthService appAuth;

    @Autowired
    DeviceTokenRepository devices;

    private String appLogin(String name) throws Exception {
        JsonNode r = read(expect(mvc.perform(post("/api/app/login/dev").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"deviceName\":\"iPhone 16\"}")), 200));
        return r.get("token").asText();
    }

    private ResultActions app(String token, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token));
    }

    @Test
    void 차단하면_그_사람_메시지와_방장_밴드가_안_보인다() throws Exception {
        Person host = person("차단방장", "[\"GUITAR\"]");
        Person rude = person("무례한멤버", "[\"BASS\"]");
        long bandId = bandWithMember(host, rude, "차단 밴드");
        expect(post_(rude, "/api/bands/" + bandId + "/messages", "{\"body\":\"보기 싫은 말\"}"), 200);

        get_(host, "/api/bands/" + bandId + "/messages").andExpect(jsonPath("$.messages[*].body", hasItem("보기 싫은 말")));
        expect(post_(host, "/api/blocks", "{\"userId\":" + rude.id() + "}"), 200);
        get_(host, "/api/blocks").andExpect(jsonPath("$[0].nickname").value("무례한멤버"));
        get_(host, "/api/bands/" + bandId + "/messages").andExpect(jsonPath("$.messages[*].body", not(hasItem("보기 싫은 말"))));

        // 무례한 사람이 연 밴드는 찾기에서 빠진다
        Person rude2 = person("무례방장", "[\"DRUM\"]");
        long rudeBand = read(expect(post_(rude2, "/api/bands", """
                {"title":"무례 밴드","genre":"ROCK","area":"합정","hostInstrument":"DRUM","seats":{"DRUM":1,"BASS":1}}
                """), 201)).get("id").asLong();
        get_(host, "/api/bands").andExpect(jsonPath("$[*].id", hasItem((int) rudeBand)));
        expect(post_(host, "/api/blocks", "{\"userId\":" + rude2.id() + "}"), 200);
        get_(host, "/api/bands").andExpect(jsonPath("$[*].id", not(hasItem((int) rudeBand))));

        expect(delete_(host, "/api/blocks/" + rude.id()), 200);
        get_(host, "/api/bands/" + bandId + "/messages").andExpect(jsonPath("$.messages[*].body", hasItem("보기 싫은 말")));
        post_(host, "/api/blocks", "{\"userId\":" + host.id() + "}").andExpect(status().isBadRequest());
    }

    @Test
    void 욕설은_메시지에서_가리고_이름에서는_막는다() throws Exception {
        Person host = person("필터방장", "[\"GUITAR\"]");
        Person bass = person("필터베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "필터 밴드");

        JsonNode m = read(expect(post_(bass, "/api/bands/" + bandId + "/messages", "{\"body\":\"아 씨발 늦었다\"}"), 200));
        assertThat(m.get("body").asText()).isEqualTo("아 ** 늦었다");
        expect(post_(bass, "/api/bands/" + bandId + "/messages", "{\"body\":\"다시 발표해요\"}"), 200);

        post_(host, "/api/bands", """
                {"title":"병신 밴드","genre":"ROCK","area":"합정","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":1}}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(startsWith("부적절한 표현")));
        put_(bass, "/api/me/profile", "{\"nickname\":\"지랄맨\",\"instruments\":[\"BASS\"],\"level\":\"HOBBY\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void 고객센터_연락처를_공개한다() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.supportEmail").value("help@bandmong.dev"));
    }

    @Test
    void 앱은_토큰으로_로그인하고_세션도_CSRF도_없이_쓴다() throws Exception {
        String token = appLogin("app-user-1");

        MvcResult me = app(token, get("/api/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("dev")).andReturn();
        assertThat(me.getRequest().getSession(false)).as("앱 요청은 세션을 만들지 않는다").isNull();

        // CSRF 토큰 없이 쓰기 요청
        app(token, put("/api/me/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"앱사용자\",\"instruments\":[\"KEYS\"],\"level\":\"HOBBY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.onboarded").value(true));

        // 실시간 연결은 헤더를 못 붙이니 쿼리로
        mvc.perform(get("/api/stream?access_token=" + token)).andExpect(request().asyncStarted());

        app("wrong-token", get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("다시 로그인해 주세요"));

        app(token, post("/api/app/logout")).andExpect(status().isNoContent());
        app(token, get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void Apple로_로그인하면_같은_Apple_계정은_같은_회원() throws Exception {
        String body = "{\"identityToken\":\"valid:apple-000123\",\"deviceName\":\"iPhone\"}";
        JsonNode a = read(expect(mvc.perform(post("/api/app/login/apple").contentType(MediaType.APPLICATION_JSON).content(body)), 200));
        JsonNode b = read(expect(mvc.perform(post("/api/app/login/apple").contentType(MediaType.APPLICATION_JSON).content(body)), 200));
        assertThat(a.get("me").get("provider").asText()).isEqualTo("apple");
        assertThat(a.get("me").get("id").asLong()).isEqualTo(b.get("me").get("id").asLong());
        assertThat(a.get("token").asText()).isNotEqualTo(b.get("token").asText()); // 기기마다 다른 토큰

        mvc.perform(post("/api/app/login/apple").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identityToken\":\"forged\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void 소셜_로그인_일회용_코드는_한_번만_토큰으로_바뀐다() throws Exception {
        String token = appLogin("code-user");
        long userId = read(app(token, get("/api/me"))).get("id").asLong();
        String code = appAuth.createLoginCode(userId);

        String body = "{\"code\":\"" + code + "\"}";
        JsonNode r = read(expect(mvc.perform(post("/api/app/login/exchange").contentType(MediaType.APPLICATION_JSON).content(body)), 200));
        assertThat(r.get("me").get("id").asLong()).isEqualTo(userId);
        mvc.perform(post("/api/app/login/exchange").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("로그인 시간이 지났어요. 다시 로그인해 주세요"));
    }

    @Test
    void 푸시_기기_등록과_탈퇴하면_모든_기기에서_로그아웃() throws Exception {
        String a = appLogin("push-a");
        String b = appLogin("push-b");
        app(a, post("/api/app/devices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-device-xyz\",\"platform\":\"IOS\"}")).andExpect(status().isNoContent());
        long aId = read(app(a, get("/api/me"))).get("id").asLong();
        assertThat(devices.findByToken("fcm-device-xyz")).get().extracting(DeviceToken::getUserId).isEqualTo(aId);

        // 같은 폰에 다른 사람이 로그인하면 주인이 바뀐다
        app(b, post("/api/app/devices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"fcm-device-xyz\",\"platform\":\"IOS\"}")).andExpect(status().isNoContent());
        assertThat(devices.findByToken("fcm-device-xyz").get().getUserId()).isNotEqualTo(aId);

        String a2 = appLogin("push-a"); // 같은 사람, 두 번째 기기
        app(a, delete("/api/me")).andExpect(status().isNoContent());
        app(a2, get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 앱_출처의_CORS_요청만_허용한다() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "capacitor://localhost")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "capacitor://localhost"));
        mvc.perform(options("/api/me").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
