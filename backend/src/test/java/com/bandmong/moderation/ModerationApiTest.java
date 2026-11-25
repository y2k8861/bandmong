package com.bandmong.moderation;

import com.bandmong.support.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ModerationApiTest extends ApiTestSupport {

    /** application-dev.yml의 bandmong.admins: dev:admin */
    private Person admin() throws Exception {
        return personNamed("admin", "운영자", "[\"ETC\"]");
    }

    private long lastMessageId(Person p, long bandId) throws Exception {
        JsonNode list = read(get_(p, "/api/bands/" + bandId + "/messages")).get("messages");
        return list.get(list.size() - 1).get("id").asLong();
    }

    private JsonNode openReportOn(Person admin, String type, long targetId) throws Exception {
        for (JsonNode r : read(get_(admin, "/api/admin/reports"))) {
            if (r.get("targetType").asText().equals(type) && r.get("targetId").asLong() == targetId) return r;
        }
        throw new AssertionError("신고 없음 " + type + " " + targetId);
    }

    @Test
    void 메시지_신고를_모아_한_번에_가린다() throws Exception {
        Person host = person("신고방장", "[\"GUITAR\"]");
        Person bad = person("나쁜말", "[\"BASS\"]");
        Person drum = person("신고드럼", "[\"DRUM\"]");
        long bandId = bandWithMember(host, bad, "신고 밴드");
        expect(post_(drum, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"DRUM\"}"), 200);
        long app = read(get_(host, "/api/bands/" + bandId)).get("applications").get(0).get("id").asLong();
        expect(post_(host, "/api/bands/" + bandId + "/applications/" + app + "/accept", null), 200);

        expect(post_(bad, "/api/bands/" + bandId + "/messages", "{\"body\":\"아주 나쁜 말\"}"), 200);
        long msgId = lastMessageId(host, bandId);
        String report = "{\"targetType\":\"MESSAGE\",\"targetId\":" + msgId + ",\"reason\":\"ABUSE\",\"detail\":\"욕설\"}";

        expect(post_(host, "/api/reports", report), 201);
        post_(host, "/api/reports", report).andExpect(status().isConflict());               // 같은 사람 두 번
        expect(post_(drum, "/api/reports", report), 201);                                    // 다른 멤버
        post_(bad, "/api/reports", report).andExpect(status().isBadRequest());              // 자기 메시지
        Person outsider = person("밖사람", "[\"KEYS\"]");
        post_(outsider, "/api/reports", report).andExpect(status().isNotFound());           // 밴드 밖 사람은 메시지를 못 봄
        get_(host, "/api/admin/reports").andExpect(status().isForbidden());                  // 관리자만

        Person admin = admin();
        JsonNode r = openReportOn(admin, "MESSAGE", msgId);
        assertThat(r.get("openOnTarget").asInt()).isEqualTo(2);
        assertThat(r.get("targetSummary").asText()).isEqualTo("아주 나쁜 말");
        assertThat(r.get("accusedNickname").asText()).isEqualTo("나쁜말");

        post_(admin, "/api/admin/reports/" + r.get("id").asLong() + "/resolve", "{\"action\":\"HIDE_MESSAGE\",\"note\":\"욕설 확인\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));

        get_(drum, "/api/bands/" + bandId + "/messages")
                .andExpect(jsonPath("$.messages[?(@.id==" + msgId + ")].body", contains("운영 정책에 따라 가려진 메시지예요")))
                .andExpect(jsonPath("$.messages[?(@.id==" + msgId + ")].hidden", contains(true)));
        assertThat(read(get_(host, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("REPORT_RESOLVED");
        assertThat(read(get_(drum, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("REPORT_RESOLVED");
        assertThat(read(get_(bad, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("SANCTIONED");
        get_(admin, "/api/admin/reports").andExpect(jsonPath("$[?(@.targetId==" + msgId + " && @.targetType=='MESSAGE')]", hasSize(0)));
    }

    @Test
    void 이용_제한_중에는_보기만_된다() throws Exception {
        Person host = person("제한방장", "[\"GUITAR\"]");
        Person bad = person("제한대상", "[\"BASS\"]");
        long bandId = bandWithMember(host, bad, "제한 밴드");
        expect(post_(host, "/api/reports", "{\"targetType\":\"USER\",\"targetId\":" + bad.id() + ",\"reason\":\"NO_SHOW\"}"), 201);

        Person admin = admin();
        long reportId = openReportOn(admin, "USER", bad.id()).get("id").asLong();
        expect(post_(admin, "/api/admin/reports/" + reportId + "/resolve", "{\"action\":\"SUSPEND_7\"}"), 200);

        post_(bad, "/api/bands/" + bandId + "/messages", "{\"body\":\"안녕\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(endsWith("까지 이용이 제한돼요. 보기만 할 수 있어요")));
        get_(bad, "/api/bands/" + bandId + "/messages").andExpect(status().isOk());
        get_(bad, "/api/me").andExpect(jsonPath("$.restrictedUntil").exists());

        expect(post_(admin, "/api/admin/users/" + bad.id() + "/lift", null), 200);
        expect(post_(bad, "/api/bands/" + bandId + "/messages", "{\"body\":\"다시 안녕\"}"), 200);
        get_(bad, "/api/me").andExpect(jsonPath("$.restrictedUntil").doesNotExist());
        post_(admin, "/api/admin/reports/" + reportId + "/resolve", "{\"action\":\"WARN\"}").andExpect(status().isConflict());
    }

    @Test
    void 방장을_넘기면_예전_방장이_나갈_수_있다() throws Exception {
        Person host = person("넘길방장", "[\"GUITAR\"]");
        Person bass = person("받을베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "넘기기 밴드");

        delete_(host, "/api/bands/" + bandId + "/members/me").andExpect(status().isConflict());
        post_(bass, "/api/bands/" + bandId + "/host", "{\"userId\":" + bass.id() + "}").andExpect(status().isForbidden());
        post_(host, "/api/bands/" + bandId + "/host", "{\"userId\":" + bass.id() + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.me").value("MEMBER"));
        get_(bass, "/api/bands/" + bandId).andExpect(jsonPath("$.me").value("HOST"));
        assertThat(read(get_(bass, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("HOST_CHANGED");
        get_(bass, "/api/bands/" + bandId + "/messages").andExpect(jsonPath("$.messages[-1].body").value("받을베이스님이 새 방장이 됐어요"));
        expect(delete_(host, "/api/bands/" + bandId + "/members/me"), 204);
    }

    @Test
    void 탈퇴하면_익명화되고_같은_이름으로_오면_새_계정() throws Exception {
        Person host = personNamed("leave-host-1", "탈퇴방장", "[\"GUITAR\"]");
        Person bass = personNamed("leave-bass-1", "탈퇴베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "탈퇴 밴드");
        expect(post_(bass, "/api/bands/" + bandId + "/messages", "{\"body\":\"그동안 고마웠어요\"}"), 200);

        delete_(host, "/api/me").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(startsWith("방장인 밴드가 있어요: '탈퇴 밴드'")));

        expect(delete_(bass, "/api/me"), 204);
        mvc.perform(get("/api/me").session(bass.session())).andExpect(status().isUnauthorized());
        get_(host, "/api/bands/" + bandId)
                .andExpect(jsonPath("$.members", hasSize(1)));
        get_(host, "/api/bands/" + bandId + "/messages")
                .andExpect(jsonPath("$.messages[?(@.body=='그동안 고마웠어요')].senderNickname", contains("탈퇴한 사용자")))
                .andExpect(jsonPath("$.messages[-1].body").value("탈퇴베이스님이 밴드를 나갔어요"));

        MockHttpSession again = loginAs("leave-bass-1");
        JsonNode me = read(mvc.perform(get("/api/me").session(again)));
        assertThat(me.get("id").asLong()).isNotEqualTo(bass.id());
        assertThat(me.get("onboarded").asBoolean()).isFalse();

        // 혼자 남은 방장은 바로 탈퇴 가능, 밴드는 모집이 닫힌다
        expect(delete_(host, "/api/me"), 204);
    }
}
