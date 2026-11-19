package com.bandmong.band;

import com.bandmong.support.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BandApiTest extends ApiTestSupport {

    /** 기타 방장 + 베이스 1, 드럼 1 자리 */
    private long createBand(Person host, String title) throws Exception {
        String body = """
                {"title":"%s","genre":"ROCK","area":"강남역 근처","schedule":"매주 금 20:00",
                 "intro":"퇴근 후 록 커버","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":1,"DRUM":1}}
                """.formatted(title);
        JsonNode band = read(post_(host, "/api/bands", body).andExpect(status().isCreated()));
        return band.get("id").asLong();
    }

    @Test
    void 밴드를_만들면_내가_방장이고_내_자리가_채워져_있다() throws Exception {
        Person host = person("기타방장", "[\"GUITAR\"]");
        long id = createBand(host, "금요일 밤 록");

        get_(host, "/api/bands/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me").value("HOST"))
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].role").value("HOST"))
                .andExpect(jsonPath("$.seats[?(@.instrument=='GUITAR')].filled", contains(1)))
                .andExpect(jsonPath("$.seats[?(@.instrument=='BASS')].filled", contains(0)))
                .andExpect(jsonPath("$.applications", hasSize(0)));
    }

    @Test
    void 자리_규칙과_프로필_없는_사람은_막는다() throws Exception {
        Person host = person("규칙방장", "[\"GUITAR\"]");
        post_(host, "/api/bands", """
                {"title":"x","genre":"ROCK","area":"판교","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":4}}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("한 악기는 3자리까지 만들 수 있어요"));
        post_(host, "/api/bands", """
                {"title":"x","genre":"ROCK","area":"판교","hostInstrument":"DRUM","seats":{"DRUM":1,"BASS":1}}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("내 프로필에 있는 악기로 골라 주세요"));
        post_(host, "/api/bands", """
                {"title":"x","genre":"ROCK","area":"판교","hostInstrument":"GUITAR","seats":{"GUITAR":1}}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("함께할 자리를 하나 이상 만들어 주세요"));
    }

    @Test
    void 지원부터_수락까지() throws Exception {
        Person host = person("수락방장", "[\"GUITAR\"]");
        Person bass = person("베이시스트", "[\"BASS\"]");
        long id = createBand(host, "수락 시나리오");

        // 지원 → 대기
        post_(bass, "/api/bands/" + id + "/applications", "{\"instrument\":\"BASS\",\"message\":\"주 1회 가능해요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me").value("PENDING"))
                .andExpect(jsonPath("$.myApplication.instrument").value("BASS"));
        // 같은 밴드에 또 지원 → 409
        post_(bass, "/api/bands/" + id + "/applications", "{\"instrument\":\"BASS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 지원했어요. 방장의 답을 기다려 주세요"));
        // 내 밴드 탭: 지원한 밴드로 보인다
        get_(bass, "/api/me/bands").andExpect(jsonPath("$.applied[*].id", hasItem((int) id)));

        // 방장: 대기 지원 1건
        JsonNode d = read(get_(host, "/api/bands/" + id));
        assertThat(d.get("applications")).hasSize(1);
        long appId = d.get("applications").get(0).get("id").asLong();
        get_(host, "/api/me/bands").andExpect(jsonPath("$.bands[?(@.id==" + id + ")].pendingCount", contains(1)));

        // 방장 아닌 사람은 수락 못 함
        post_(bass, "/api/bands/" + id + "/applications/" + appId + "/accept", null)
                .andExpect(status().isForbidden());

        post_(host, "/api/bands/" + id + "/applications/" + appId + "/accept", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.applications", hasSize(0)));
        // 두 번 수락 → 409
        post_(host, "/api/bands/" + id + "/applications/" + appId + "/accept", null)
                .andExpect(status().isConflict());

        get_(bass, "/api/bands/" + id).andExpect(jsonPath("$.me").value("MEMBER"));
        get_(bass, "/api/me/bands").andExpect(jsonPath("$.bands[?(@.id==" + id + ")].myInstrument", contains("BASS")));
    }

    @Test
    void 지원_조건() throws Exception {
        Person host = person("조건방장", "[\"GUITAR\"]");
        Person keys = person("키보디스트", "[\"KEYS\",\"GUITAR\"]");
        long id = createBand(host, "조건 시나리오");

        post_(keys, "/api/bands/" + id + "/applications", "{\"instrument\":\"BASS\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("내 프로필에 있는 악기로 지원해 주세요"));
        post_(keys, "/api/bands/" + id + "/applications", "{\"instrument\":\"KEYS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("키보드 자리는 지금 비어 있지 않아요"));
        post_(keys, "/api/bands/" + id + "/applications", "{\"instrument\":\"GUITAR\"}")
                .andExpect(status().isConflict()); // 기타 1자리는 방장이 차지

        patch_(host, "/api/bands/" + id, "{\"recruiting\":false}").andExpect(status().isOk());
        Person drum = person("드러머", "[\"DRUM\"]");
        post_(drum, "/api/bands/" + id + "/applications", "{\"instrument\":\"DRUM\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("지금은 멤버를 모집하지 않아요"));
    }

    @Test
    void 거절과_지원취소() throws Exception {
        Person host = person("거절방장", "[\"GUITAR\"]");
        Person a = person("지원자에이", "[\"BASS\"]");
        Person b = person("지원자비", "[\"DRUM\"]");
        long id = createBand(host, "거절 시나리오");

        post_(a, "/api/bands/" + id + "/applications", "{\"instrument\":\"BASS\"}").andExpect(status().isOk());
        post_(b, "/api/bands/" + id + "/applications", "{\"instrument\":\"DRUM\"}").andExpect(status().isOk());
        long appA = read(get_(host, "/api/bands/" + id)).get("applications").get(0).get("id").asLong();

        post_(host, "/api/bands/" + id + "/applications/" + appA + "/reject", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(1)));
        get_(a, "/api/bands/" + id).andExpect(jsonPath("$.me").value("NONE"));

        delete_(b, "/api/bands/" + id + "/applications/mine")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me").value("NONE"));
        get_(host, "/api/bands/" + id).andExpect(jsonPath("$.applications", hasSize(0)));
    }

    @Test
    void 찾기는_빈자리_있는_모집중_밴드만_보여주고_악기로_거른다() throws Exception {
        Person host = person("찾기방장", "[\"GUITAR\"]");
        Person viewer = person("구경꾼", "[\"VOCAL\"]");
        long id = createBand(host, "찾기 시나리오");

        get_(viewer, "/api/bands?instrument=BASS").andExpect(jsonPath("$[*].id", hasItem((int) id)));
        get_(viewer, "/api/bands?instrument=VOCAL").andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        get_(viewer, "/api/bands?genre=JAZZ").andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        get_(viewer, "/api/bands?area=강남").andExpect(jsonPath("$[*].id", hasItem((int) id)));

        // 자리를 방장 것만 남기면(빈자리 0) 목록에서 빠진다
        patch_(host, "/api/bands/" + id, "{\"seats\":{\"GUITAR\":1}}").andExpect(status().isOk());
        get_(viewer, "/api/bands").andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
    }

    @Test
    void 멤버가_있는_자리는_줄일_수_없고_멤버는_나갈_수_있다() throws Exception {
        Person host = person("자리방장", "[\"GUITAR\"]");
        Person bass = person("나갈베이스", "[\"BASS\"]");
        long id = createBand(host, "자리 시나리오");
        post_(bass, "/api/bands/" + id + "/applications", "{\"instrument\":\"BASS\"}");
        long appId = read(get_(host, "/api/bands/" + id)).get("applications").get(0).get("id").asLong();
        post_(host, "/api/bands/" + id + "/applications/" + appId + "/accept", null).andExpect(status().isOk());

        patch_(host, "/api/bands/" + id, "{\"seats\":{\"GUITAR\":1,\"DRUM\":1}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("베이스 자리는 이미 멤버가 있어 1개보다 줄일 수 없어요"));

        delete_(host, "/api/bands/" + id + "/members/me").andExpect(status().isConflict());
        delete_(bass, "/api/bands/" + id + "/members/me").andExpect(status().isNoContent());
        get_(bass, "/api/bands/" + id).andExpect(jsonPath("$.me").value("NONE"));
    }

    @Test
    void 프로필_설정_전에는_밴드를_만들_수_없다() throws Exception {
        var s = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/dev/login")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"name\":\"no-profile\"}"))
                .andReturn().getRequest().getSession(false);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/bands")
                        .session((org.springframework.mock.web.MockHttpSession) s)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"genre\":\"ROCK\",\"area\":\"a\",\"hostInstrument\":\"GUITAR\",\"seats\":{\"GUITAR\":1,\"BASS\":1}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("프로필을 먼저 설정해 주세요"));
    }
}
