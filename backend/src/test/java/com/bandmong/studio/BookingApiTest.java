package com.bandmong.studio;

import com.bandmong.config.TimeConfig;
import com.bandmong.studio.dto.BookingRequest;
import com.bandmong.support.ApiTestSupport;
import com.bandmong.web.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest extends ApiTestSupport {

    @Autowired
    BookingService service;

    /** 합주실 이름·방 순서로 방 id 찾기 */
    private long roomId(Person p, String studio, int index) throws Exception {
        for (JsonNode s : read(get_(p, "/api/studios"))) {
            if (s.get("name").asText().equals(studio)) return s.get("rooms").get(index).get("id").asLong();
        }
        throw new IllegalStateException(studio);
    }

    private static LocalDate day(int plus) {
        return LocalDate.now(TimeConfig.KST).plusDays(plus);
    }

    private String body(long roomId, LocalDate date, int start, int hours) {
        return "{\"roomId\":" + roomId + ",\"date\":\"" + date + "\",\"startHour\":" + start + ",\"hours\":" + hours + "}";
    }

    @Test
    void 합주실_목록과_시간표_밤_할인() throws Exception {
        Person p = person("목록보기", "[\"GUITAR\"]");
        JsonNode list = read(get_(p, "/api/studios"));
        assertThat(list.size()).isGreaterThanOrEqualTo(7);
        long room = roomId(p, "합정 그루브 합주실", 0);

        JsonNode slots = read(get_(p, "/api/rooms/" + room + "/slots?date=" + day(10))).get("slots");
        assertThat(slots.get(0).get("hour").asInt()).isEqualTo(10);
        assertThat(slots.get(slots.size() - 1).get("hour").asInt()).isEqualTo(25); // 새벽 1시 칸까지
        for (JsonNode s : slots) {
            int expected = s.get("hour").asInt() >= 22 ? 18000 : 20000;
            assertThat(s.get("price").asInt()).isEqualTo(expected);
        }
    }

    @Test
    void 예약하면_시간이_막히고_1인_정산이_생긴다() throws Exception {
        Person host = person("예약방장", "[\"GUITAR\"]");
        Person bass = person("예약베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "예약 밴드");
        long room = roomId(host, "합정 그루브 합주실", 0);
        LocalDate d = day(5);

        JsonNode b = read(expect(post_(host, "/api/bands/" + bandId + "/bookings", body(room, d, 21, 2)), 201));
        assertThat(b.get("price").asInt()).isEqualTo(20000 + 18000);   // 21시 + 22시(할인)
        assertThat(b.get("perPerson").asInt()).isEqualTo(19000);
        assertThat(b.get("settles")).hasSize(2);
        long bookingId = b.get("id").asLong();

        JsonNode slots = read(get_(host, "/api/rooms/" + room + "/slots?date=" + d)).get("slots");
        for (JsonNode s : slots) {
            int h = s.get("hour").asInt();
            assertThat(s.get("taken").asBoolean()).as("%d시", h).isEqualTo(h == 21 || h == 22);
        }

        // 다른 밴드가 겹치는 시간 → 409
        Person other = person("다른방장", "[\"DRUM\"]");
        Person otherBass = person("다른베이스", "[\"BASS\"]");
        long otherBand = read(expect(post_(other, "/api/bands", """
                {"title":"다른 밴드","genre":"ROCK","area":"합정","hostInstrument":"DRUM","seats":{"DRUM":1,"BASS":1}}
                """), 201)).get("id").asLong();
        post_(other, "/api/bands/" + otherBand + "/bookings", body(room, d, 22, 1))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("그 시간은 이미 예약돼 있어요"));

        // 규칙: 멤버는 예약 못 함, 영업시간 밖, 지난 시간
        post_(bass, "/api/bands/" + bandId + "/bookings", body(room, d, 18, 1)).andExpect(status().isForbidden());
        post_(host, "/api/bands/" + bandId + "/bookings", body(room, d, 8, 2))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("영업시간 안으로 골라 주세요 (10:00~02:00)"));
        post_(host, "/api/bands/" + bandId + "/bookings", body(room, day(-1), 20, 1)).andExpect(status().isBadRequest());

        // 정산: 내 몫은 내가, 남의 몫은 방장만
        put_(bass, "/api/bookings/" + bookingId + "/settles/" + bass.id(), "{\"paid\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.paidCount").value(1)).andExpect(jsonPath("$.mine.paid").value(true));
        put_(bass, "/api/bookings/" + bookingId + "/settles/" + host.id(), "{\"paid\":true}").andExpect(status().isForbidden());
        put_(host, "/api/bookings/" + bookingId + "/settles/" + bass.id(), "{\"paid\":false}")
                .andExpect(jsonPath("$.paidCount").value(0));

        // 채팅 안내 + 멤버 알림
        get_(bass, "/api/bands/" + bandId + "/messages")
                .andExpect(jsonPath("$.messages[-1].body").value(org.hamcrest.Matchers.containsString("1인 19,000원")));
        assertThat(read(get_(bass, "/api/notifications")).get("items").get(0).get("type").asText()).isEqualTo("BOOKING_CONFIRMED");

        // 취소하면 칸이 풀려 다른 밴드가 잡을 수 있다
        post_(host, "/api/bookings/" + bookingId + "/cancel", null).andExpect(jsonPath("$.status").value("CANCELED"));
        expect(post_(other, "/api/bands/" + otherBand + "/bookings", body(room, d, 22, 1)), 201);
        assertThat(otherBass).isNotNull();
    }

    @Test
    void 두_밴드가_같은_방_같은_시간을_동시에_잡으면_한_밴드만() throws Exception {
        Person a = person("동시예약가", "[\"GUITAR\"]");
        Person b = person("동시예약나", "[\"GUITAR\"]");
        String band = """
                {"title":"동시 예약","genre":"ROCK","area":"성수","hostInstrument":"GUITAR","seats":{"GUITAR":1,"BASS":1}}
                """;
        long bandA = read(expect(post_(a, "/api/bands", band), 201)).get("id").asLong();
        long bandB = read(expect(post_(b, "/api/bands", band), 201)).get("id").asLong();
        long room = roomId(a, "성수 언더그라운드", 0);
        BookingRequest req = new BookingRequest(room, day(7), 20, 2, null);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), lost = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (long[] who : new long[][]{{a.id(), bandA}, {b.id(), bandB}}) {
            fs.add(pool.submit(() -> {
                go.await();
                try {
                    service.book(who[0], who[1], req);
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) lost.incrementAndGet();
                    else throw e;
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(lost.get()).isEqualTo(1);
        long taken = 0;
        for (JsonNode s : read(get_(a, "/api/rooms/" + room + "/slots?date=" + day(7))).get("slots")) {
            if (s.get("taken").asBoolean()) taken++;
        }
        assertThat(taken).isEqualTo(2);
    }
}
