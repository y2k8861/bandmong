package com.bandmong.band;

import com.bandmong.support.ApiTestSupport;
import com.bandmong.web.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 베이스 한 자리에 두 명이 지원했고, 방장이 (예: 두 기기에서) 둘을 거의 동시에 수락한다.
 * 밴드 행 잠금이 없으면 둘 다 "빈자리 1개"를 보고 둘 다 들어간다. 잠금이 있으면 한 명만 들어가야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LastSeatConcurrencyTest extends ApiTestSupport {

    @Autowired
    BandService service;

    @Autowired
    BandMemberRepository members;

    @Test
    void 마지막_자리에_동시_수락하면_한명만_들어간다() throws Exception {
        Person host = person("동시방장", "[\"GUITAR\"]");
        Person a = person("베이스에이", "[\"BASS\"]");
        Person b = person("베이스비", "[\"BASS\"]");
        long bandId = read(post_(host, "/api/bands", """
                {"title":"동시성","genre":"INDIE","area":"을지로","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":1}}
                """).andExpect(status().isCreated())).get("id").asLong();
        post_(a, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"BASS\"}").andExpect(status().isOk());
        post_(b, "/api/bands/" + bandId + "/applications", "{\"instrument\":\"BASS\"}").andExpect(status().isOk());

        List<Long> appIds = new ArrayList<>();
        for (JsonNode app : read(get_(host, "/api/bands/" + bandId)).get("applications")) {
            appIds.add(app.get("id").asLong());
        }
        assertThat(appIds).hasSize(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger full = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (Long appId : appIds) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.accept(host.id(), bandId, appId);
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) full.incrementAndGet();
                    else throw e;
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(20, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(full.get()).isEqualTo(1);
        long bassMembers = members.findByBandWithUser(bandId).stream()
                .filter(m -> m.getInstrument() == com.bandmong.user.Instrument.BASS).count();
        assertThat(bassMembers).isEqualTo(1);
    }
}
