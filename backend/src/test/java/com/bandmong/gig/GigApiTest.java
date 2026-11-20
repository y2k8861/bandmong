package com.bandmong.gig;

import com.bandmong.config.TimeConfig;
import com.bandmong.gig.dto.GigRequests;
import com.bandmong.support.ApiTestSupport;
import com.bandmong.web.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GigApiTest extends ApiTestSupport {

    @Autowired
    GigService service;

    private static String when(int days) {
        return LocalDate.now(TimeConfig.KST).plusDays(days).atTime(19, 30).atZone(TimeConfig.KST).toOffsetDateTime().toString();
    }

    private JsonNode createGig(Person host, long bandId) throws Exception {
        return read(expect(post_(host, "/api/bands/" + bandId + "/gigs",
                "{\"title\":\"연말 클럽 공연\",\"startsAt\":\"" + when(30) + "\",\"venue\":\"홍대 클럽 FF\",\"slotMinutes\":20}"), 201));
    }

    private static String items(long version, String rows) {
        return "{\"version\":" + version + ",\"items\":[" + rows + "]}";
    }

    @Test
    void 공연을_잡으면_큐시트_초안과_준비목록이_생기고_멤버에게_알린다() throws Exception {
        Person host = person("공연방장", "[\"GUITAR\"]");
        Person bass = person("공연베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "공연 밴드");
        JsonNode g = createGig(host, bandId);

        assertThat(g.get("items")).hasSize(2);
        assertThat(g.get("checks")).hasSize(5);
        assertThat(g.get("totalSec").asInt()).isEqualTo(105);
        assertThat(read(get_(bass, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("GIG_SCHEDULED");
        get_(bass, "/api/bands/" + bandId + "/messages")
                .andExpect(jsonPath("$.messages[-1].body").value(org.hamcrest.Matchers.startsWith("공연이 잡혔어요 · 연말 클럽 공연")));
        get_(bass, "/api/bands/" + bandId + "/gigs").andExpect(jsonPath("$[0].title").value("연말 클럽 공연"));

        post_(bass, "/api/bands/" + bandId + "/gigs",
                "{\"title\":\"x\",\"startsAt\":\"" + when(10) + "\",\"slotMinutes\":20}").andExpect(status().isForbidden());
        Person other = person("공연남", "[\"DRUM\"]");
        get_(other, "/api/gigs/" + g.get("id").asLong()).andExpect(status().isForbidden());
    }

    @Test
    void 오래된_버전으로_큐시트를_저장하면_409() throws Exception {
        Person host = person("큐방장", "[\"GUITAR\"]");
        Person bass = person("큐베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "큐시트 밴드");
        JsonNode g = createGig(host, bandId);
        long gigId = g.get("id").asLong(), v0 = g.get("version").asLong();
        String url = "/api/gigs/" + gigId + "/items";

        JsonNode saved = read(expect(put_(bass, url, items(v0,
                "{\"type\":\"MC\",\"title\":\"인사\",\"durationSec\":60},{\"type\":\"SONG\",\"title\":\"첫 곡\",\"durationSec\":240}")), 200));
        assertThat(saved.get("version").asLong()).isGreaterThan(v0);
        assertThat(saved.get("items").get(1).get("title").asText()).isEqualTo("첫 곡");

        // 방장은 아직 예전 화면(v0)을 보고 있다 → 덮어쓰지 못한다
        put_(host, url, items(v0, "{\"type\":\"SONG\",\"title\":\"다른 곡\",\"durationSec\":200}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(GigService.STALE));

        // 체크리스트를 체크해도 큐시트 버전은 그대로
        long v1 = saved.get("version").asLong();
        long checkId = saved.get("checks").get(0).get("id").asLong();
        JsonNode checked = read(expect(put_(host, "/api/gigs/" + gigId + "/checks/" + checkId, "{\"done\":true}"), 200));
        assertThat(checked.get("doneCount").asInt()).isEqualTo(1);
        assertThat(checked.get("checks").get(0).get("doneBy").asText()).isEqualTo("큐방장");
        assertThat(checked.get("version").asLong()).isEqualTo(v1);
        expect(post_(bass, "/api/gigs/" + gigId + "/checks", "{\"text\":\"인이어 배터리\"}"), 200);
        expect(put_(host, url, items(v1, "{\"type\":\"SONG\",\"title\":\"첫 곡\",\"durationSec\":255}")), 200);

        // 검사
        put_(host, url, items(read(get_(host, "/api/gigs/" + gigId)).get("version").asLong(),
                "{\"type\":\"SONG\",\"title\":\"너무 짧음\",\"durationSec\":10}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("한 줄은 15초 이상이에요"));
        expect(post_(host, "/api/gigs/" + gigId + "/cancel", null), 200);
        put_(bass, url, items(read(get_(bass, "/api/gigs/" + gigId)).get("version").asLong(), ""))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("취소된 공연이에요"));
    }

    @Test
    void 같은_버전으로_동시에_저장하면_한_명만_성공() throws Exception {
        Person host = person("동시큐방장", "[\"GUITAR\"]");
        Person bass = person("동시큐베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "동시 큐시트");
        JsonNode g = createGig(host, bandId);
        long gigId = g.get("id").asLong(), v = g.get("version").asLong();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), stale = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (Person p : List.of(host, bass)) {
            fs.add(pool.submit(() -> {
                go.await();
                try {
                    service.saveItems(p.id(), gigId, new GigRequests.Items(v,
                            List.of(new GigRequests.Item(CueType.SONG, p.id() + "의 곡", 200))));
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) stale.incrementAndGet(); else throw e;
                } catch (ObjectOptimisticLockingFailureException e) {
                    stale.incrementAndGet(); // 확인은 둘 다 통과했지만 @Version이 막은 경우
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(stale.get()).isEqualTo(1);
        assertThat(read(get_(host, "/api/gigs/" + gigId)).get("items")).hasSize(1);
    }
}
