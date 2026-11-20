package com.bandmong.dues;

import com.bandmong.config.TimeConfig;
import com.bandmong.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DuesApiTest extends ApiTestSupport {

    @Test
    void 회비를_정하고_낸_사람을_표시한다() throws Exception {
        Person host = person("회비방장", "[\"GUITAR\"]");
        Person bass = person("회비베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "회비 밴드");
        String month = YearMonth.now(TimeConfig.KST).toString();
        String url = "/api/bands/" + bandId + "/dues";

        get_(bass, url).andExpect(jsonPath("$.amount").doesNotExist()).andExpect(jsonPath("$.month").value(month));
        put_(bass, url + "/" + month + "/members/" + bass.id(), "{\"paid\":true}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("회비가 정해져 있지 않아요"));
        put_(bass, url + "/settings", "{\"amount\":30000}").andExpect(status().isForbidden());

        put_(host, url + "/settings", "{\"amount\":30000,\"account\":\"카카오뱅크 3333-01-1234567 김방장\"}")
                .andExpect(jsonPath("$.amount").value(30000))
                .andExpect(jsonPath("$.paidCount").value(0))
                .andExpect(jsonPath("$.total").value(2));

        put_(bass, url + "/" + month + "/members/" + bass.id(), "{\"paid\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.paidCount").value(1));
        put_(bass, url + "/" + month + "/members/" + bass.id(), "{\"paid\":true}")
                .andExpect(jsonPath("$.paidCount").value(1)); // 두 번 눌러도 한 번
        put_(bass, url + "/" + month + "/members/" + host.id(), "{\"paid\":true}").andExpect(status().isForbidden());
        put_(host, url + "/" + month + "/members/" + bass.id(), "{\"paid\":false}")
                .andExpect(jsonPath("$.paidCount").value(0));

        get_(host, url + "?month=2026-13").andExpect(status().isBadRequest());
        get_(host, url + "?month=" + YearMonth.now(TimeConfig.KST).plusMonths(2)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("다음 달까지만 볼 수 있어요"));
        Person other = person("회비남", "[\"DRUM\"]");
        get_(other, url).andExpect(status().isForbidden());
    }
}
