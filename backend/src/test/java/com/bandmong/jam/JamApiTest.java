package com.bandmong.jam;

import com.bandmong.config.TimeConfig;
import com.bandmong.support.ApiTestSupport;
import com.bandmong.support.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JamApiTest extends ApiTestSupport {

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    /** 방장(기타) + 멤버(베이스)가 있는 밴드 */
    record Setup(Person host, Person member, long bandId) {
    }

    private Setup band() throws Exception {
        Person host = person("합주방장", "[\"GUITAR\"]");
        Person member = person("합주베이스", "[\"BASS\"]");
        long bandId = read(expect(post_(host, "/api/bands", """
                {"title":"합주 테스트","genre":"ROCK","area":"합정","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":1,"DRUM":1}}
                """), 201)).get("id").asLong();
        expect(post_(member, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"BASS\"}"), 200);
        long appId = read(get_(host, "/api/bands/" + bandId)).get("applications").get(0).get("id").asLong();
        expect(post_(host, "/api/bands/" + bandId + "/applications/" + appId + "/accept", null), 200);
        return new Setup(host, member, bandId);
    }

    private static LocalDate nextMonday() {
        return LocalDate.now(TimeConfig.KST).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /** n일 뒤 한국 시간 20:00 */
    private static String kst(int daysLater, int hour) {
        return LocalDate.now(TimeConfig.KST).plusDays(daysLater).atTime(hour, 0)
                .atZone(TimeConfig.KST).toOffsetDateTime().toString();
    }

    private long createJam(Setup s, int daysLater) throws Exception {
        return read(expect(post_(s.host(), "/api/bands/" + s.bandId() + "/jams",
                "{\"startsAt\":\"" + kst(daysLater, 20) + "\",\"durationMinutes\":120,\"place\":\"합정 A룸\"}"), 201))
                .get("id").asLong();
    }

    @Test
    void 가능_시간_투표와_가장_많이_겹치는_시간() throws Exception {
        Setup s = band();
        String week = nextMonday().toString();
        int fri20 = 4 * 24 + 20, fri21 = 4 * 24 + 21, sat14 = 5 * 24 + 14;
        String url = "/api/bands/" + s.bandId() + "/availability";

        expect(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .session(s.member().session())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{\"weekStart\":\"" + week + "\",\"slots\":[" + fri20 + "," + fri21 + "]}")), 200);
        JsonNode v = read(putJson(s.host(), url, "{\"weekStart\":\"" + week + "\",\"slots\":[" + fri20 + "]}"));

        assertThat(v.get("counts").get(String.valueOf(fri20)).asInt()).isEqualTo(2);
        assertThat(v.get("counts").get(String.valueOf(fri21)).asInt()).isEqualTo(1);
        assertThat(v.get("best").get(0).get("slot").asInt()).isEqualTo(fri20);
        assertThat(v.get("best").get(0).get("names")).hasSize(2);
        assertThat(v.get("voters")).allSatisfy(x -> assertThat(x.get("voted").asBoolean()).isTrue());

        // 겹치는 칸이 있는 새 목록으로 바꿔도 된다 (빠진 칸만 지우고 새 칸만 넣음)
        JsonNode again = read(putJson(s.member(), url, "{\"weekStart\":\"" + week + "\",\"slots\":[" + fri20 + "," + sat14 + "]}"));
        assertThat(again.get("mine")).extracting(JsonNode::asInt).containsExactly(fri20, sat14);
        assertThat(again.get("counts").has(String.valueOf(fri21))).isFalse();

        // 규칙
        putJson(s.member(), url, "{\"weekStart\":\"" + week + "\",\"slots\":[" + (4 * 24 + 15) + "]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("고를 수 없는 시간이 있어요"));
        putJson(s.member(), url, "{\"weekStart\":\"" + nextMonday().plusDays(1) + "\",\"slots\":[]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("주 시작일은 월요일이어야 해요"));
        Person stranger = person("남의밴드", "[\"DRUM\"]");
        get_(stranger, url).andExpect(status().isForbidden());
    }

    @Test
    void 방장이_합주를_확정하고_멤버가_참석_응답() throws Exception {
        Setup s = band();
        long jamId = createJam(s, 3);

        get_(s.member(), "/api/jams/" + jamId)
                .andExpect(jsonPath("$.counts.yes").value(1))   // 방장은 확정과 함께 참석
                .andExpect(jsonPath("$.counts.none").value(1))
                .andExpect(jsonPath("$.host").value(false))
                .andExpect(jsonPath("$.answers", hasSize(2)));

        putJson(s.member(), "/api/jams/" + jamId + "/rsvp", "{\"answer\":\"LATE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myAnswer").value("LATE"))
                .andExpect(jsonPath("$.counts.late").value(1));
        putJson(s.member(), "/api/jams/" + jamId + "/rsvp", "{\"answer\":\"NO\"}")
                .andExpect(jsonPath("$.counts.late").value(0))
                .andExpect(jsonPath("$.counts.no").value(1));

        // 멤버는 확정 못 함, 지난 시간·겹치는 시간 안 됨
        post_(s.member(), "/api/bands/" + s.bandId() + "/jams", "{\"startsAt\":\"" + kst(4, 20) + "\",\"durationMinutes\":120}")
                .andExpect(status().isForbidden());
        post_(s.host(), "/api/bands/" + s.bandId() + "/jams", "{\"startsAt\":\"" + kst(-1, 20) + "\",\"durationMinutes\":120}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("지난 시간으로는 잡을 수 없어요"));
        post_(s.host(), "/api/bands/" + s.bandId() + "/jams", "{\"startsAt\":\"" + kst(3, 21) + "\",\"durationMinutes\":60}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("그 시간에 이미 잡힌 합주가 있어요"));

        // 내 밴드 카드에 다음 합주와 내 응답
        get_(s.member(), "/api/me/bands")
                .andExpect(jsonPath("$.bands[?(@.id==" + s.bandId() + ")].nextJam.id", contains((int) jamId)))
                .andExpect(jsonPath("$.bands[?(@.id==" + s.bandId() + ")].nextJam.myAnswer", contains("NO")));
        get_(s.member(), "/api/bands/" + s.bandId()).andExpect(jsonPath("$.nextJam.id").value((int) jamId));
    }

    @Test
    void 시간을_바꾸면_응답을_다시_받고_장소만_바꾸면_유지() throws Exception {
        Setup s = band();
        long jamId = createJam(s, 3);
        putJson(s.member(), "/api/jams/" + jamId + "/rsvp", "{\"answer\":\"YES\"}");

        patchJson(s.host(), "/api/jams/" + jamId, "{\"place\":\"망원 B룸\"}")
                .andExpect(jsonPath("$.place").value("망원 B룸"))
                .andExpect(jsonPath("$.counts.yes").value(2));

        patchJson(s.host(), "/api/jams/" + jamId, "{\"startsAt\":\"" + kst(5, 19) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.yes").value(1))
                .andExpect(jsonPath("$.counts.none").value(1));
        get_(s.member(), "/api/jams/" + jamId).andExpect(jsonPath("$.myAnswer").doesNotExist());

        patchJson(s.member(), "/api/jams/" + jamId, "{\"place\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    void 취소된_합주에는_답할_수_없고_목록에는_남는다() throws Exception {
        Setup s = band();
        long jamId = createJam(s, 2);
        post_(s.host(), "/api/jams/" + jamId + "/cancel", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
        putJson(s.member(), "/api/jams/" + jamId + "/rsvp", "{\"answer\":\"YES\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("취소된 합주예요"));
        get_(s.member(), "/api/bands/" + s.bandId() + "/jams")
                .andExpect(jsonPath("$.upcoming[0].status").value("CANCELED"));
        get_(s.member(), "/api/me/bands")
                .andExpect(jsonPath("$.bands[?(@.id==" + s.bandId() + ")].nextJam", contains(nullValue())));
    }

    @Test
    void 시간이_지나면_지난_합주로_옮겨지고_바꿀_수_없다() throws Exception {
        Setup s = band();
        long jamId = createJam(s, 1);
        clock.advance(Duration.ofDays(2));

        get_(s.member(), "/api/bands/" + s.bandId() + "/jams")
                .andExpect(jsonPath("$.upcoming", hasSize(0)))
                .andExpect(jsonPath("$.past[0].id").value((int) jamId))
                .andExpect(jsonPath("$.past[0].over").value(true));
        putJson(s.member(), "/api/jams/" + jamId + "/rsvp", "{\"answer\":\"YES\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 시작한 합주는 답을 바꿀 수 없어요"));
        patchJson(s.host(), "/api/jams/" + jamId, "{\"place\":\"x\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 끝난 합주예요"));
    }

    private org.springframework.test.web.servlet.ResultActions putJson(Person p, String url, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .session(p.session())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content(body));
    }

    private org.springframework.test.web.servlet.ResultActions patchJson(Person p, String url, String body) throws Exception {
        return patch_(p, url, body);
    }
}
