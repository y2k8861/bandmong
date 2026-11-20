package com.bandmong.notification;

import com.bandmong.config.TimeConfig;
import com.bandmong.support.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiTest extends ApiTestSupport {

    private JsonNode latest(Person p) throws Exception {
        return read(get_(p, "/api/notifications")).get("items").get(0);
    }

    private long unread(Person p) throws Exception {
        return read(get_(p, "/api/notifications/unread")).get("unread").asLong();
    }

    private static String kst(int days, int hour) {
        return LocalDate.now(TimeConfig.KST).plusDays(days).atTime(hour, 0).atZone(TimeConfig.KST).toOffsetDateTime().toString();
    }

    @Test
    void 지원과_결과를_알려준다() throws Exception {
        Person host = person("알림방장", "[\"GUITAR\"]");
        Person bass = person("알림베이스", "[\"BASS\"]");
        Person drum = person("알림드럼", "[\"DRUM\"]");
        long bandId = bandWithMember(host, bass, "알림 밴드");

        JsonNode toHost = read(get_(host, "/api/notifications")).get("items").get(0);
        assertThat(toHost.get("type").asText()).isEqualTo("APPLICATION_RECEIVED");
        assertThat(toHost.get("body").asText()).isEqualTo("알림베이스님이 '알림 밴드'에 베이스로 지원했어요");

        JsonNode toBass = latest(bass);
        assertThat(toBass.get("type").asText()).isEqualTo("APPLICATION_ACCEPTED");
        assertThat(toBass.get("link").asText()).isEqualTo("/bands/" + bandId + "/chat");

        expect(post_(drum, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"DRUM\"}"), 200);
        assertThat(latest(host).get("body").asText()).contains("드럼으로 지원했어요");
        long appId = read(get_(host, "/api/bands/" + bandId)).get("applications").get(0).get("id").asLong();
        expect(post_(host, "/api/bands/" + bandId + "/applications/" + appId + "/reject", null), 200);
        assertThat(latest(drum).get("type").asText()).isEqualTo("APPLICATION_REJECTED");

        // 나가면 방장에게
        expect(delete_(bass, "/api/bands/" + bandId + "/members/me"), 204);
        assertThat(latest(host).get("type").asText()).isEqualTo("MEMBER_LEFT");
    }

    @Test
    void 합주_확정_변경_취소는_방장을_뺀_멤버에게() throws Exception {
        Person host = person("합알방장", "[\"GUITAR\"]");
        Person bass = person("합알베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "합주 알림 밴드");
        long hostBefore = unread(host);

        long jamId = read(expect(post_(host, "/api/bands/" + bandId + "/jams",
                "{\"startsAt\":\"" + kst(3, 20) + "\",\"durationMinutes\":120,\"place\":\"합정 A룸\"}"), 201)).get("id").asLong();
        JsonNode n = latest(bass);
        assertThat(n.get("type").asText()).isEqualTo("JAM_SCHEDULED");
        assertThat(n.get("link").asText()).isEqualTo("/jams/" + jamId);
        assertThat(n.get("body").asText()).contains("20:00 · 합정 A룸");
        assertThat(unread(host)).isEqualTo(hostBefore); // 한 사람에게는 안 감

        get_(bass, "/api/bands/" + bandId + "/messages")
                .andExpect(jsonPath("$.messages[-1].body").value(org.hamcrest.Matchers.startsWith("합주가 잡혔어요 · ")));

        expect(patch_(host, "/api/jams/" + jamId, "{\"place\":\"망원 B룸\"}"), 200);
        assertThat(latest(bass).get("type").asText()).isEqualTo("JAM_EDITED");
        expect(patch_(host, "/api/jams/" + jamId, "{\"place\":\"망원 B룸\"}"), 200); // 바뀐 게 없으면 알림 없음
        expect(patch_(host, "/api/jams/" + jamId, "{\"startsAt\":\"" + kst(4, 19) + "\"}"), 200);
        assertThat(latest(bass).get("type").asText()).isEqualTo("JAM_MOVED");
        expect(post_(host, "/api/jams/" + jamId + "/cancel", null), 200);
        assertThat(latest(bass).get("type").asText()).isEqualTo("JAM_CANCELED");

        JsonNode page = read(get_(bass, "/api/notifications"));
        long edited = 0;
        for (JsonNode x : page.get("items")) if (x.get("type").asText().equals("JAM_EDITED")) edited++;
        assertThat(edited).isEqualTo(1);
    }

    @Test
    void 읽음_처리와_남의_알림() throws Exception {
        Person host = person("읽음방장", "[\"GUITAR\"]");
        Person bass = person("읽음베이스", "[\"BASS\"]");
        bandWithMember(host, bass, "읽음 밴드");

        long id = latest(host).get("id").asLong();
        assertThat(unread(host)).isPositive();
        post_(bass, "/api/notifications/" + id + "/read", null).andExpect(status().isNotFound());
        post_(host, "/api/notifications/" + id + "/read", null).andExpect(jsonPath("$.read").value(true));
        expect(post_(host, "/api/notifications/read-all", null), 200);
        assertThat(unread(host)).isZero();
    }
}
