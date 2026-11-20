package com.bandmong.chat;

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
class ChatApiTest extends ApiTestSupport {

    private int unread(Person p, long bandId) throws Exception {
        return read(get_(p, "/api/bands/" + bandId)).get("unreadMessages").asInt();
    }

    @Test
    void 들어오면_안내_메시지가_남고_대화와_안읽은_수가_맞는다() throws Exception {
        Person host = person("채팅방장", "[\"GUITAR\"]");
        Person bass = person("채팅베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "채팅 밴드");
        String url = "/api/bands/" + bandId + "/messages";

        get_(bass, url)
                .andExpect(jsonPath("$.messages[-1].type").value("SYSTEM"))
                .andExpect(jsonPath("$.messages[-1].body").value("채팅베이스님이 베이스로 들어왔어요"));

        JsonNode sent = read(expect(post_(bass, url, "{\"body\":\" 안녕하세요! \",\"clientId\":\"c-0001-abcd\"}"), 200));
        assertThat(sent.get("body").asText()).isEqualTo("안녕하세요!");
        assertThat(sent.get("senderNickname").asText()).isEqualTo("채팅베이스");

        // 응답을 못 받아 다시 보내도 한 번만 저장
        JsonNode again = read(expect(post_(bass, url, "{\"body\":\"안녕하세요!\",\"clientId\":\"c-0001-abcd\"}"), 200));
        assertThat(again.get("id").asLong()).isEqualTo(sent.get("id").asLong());

        assertThat(unread(host, bandId)).isEqualTo(2);  // 들어왔어요 + 안녕하세요
        assertThat(unread(bass, bandId)).isEqualTo(0);  // 내가 보낸 건 읽은 것
        get_(host, "/api/me/bands").andExpect(jsonPath("$.bands[?(@.id==" + bandId + ")].unreadMessages", contains(2)));

        expect(post_(host, url + "/read", "{\"lastId\":" + sent.get("id").asLong() + "}"), 204);
        assertThat(unread(host, bandId)).isZero();

        // 없는 미래 id로 읽음 처리해도, 그 뒤에 온 메시지는 안 읽은 것으로 남는다
        expect(post_(host, url + "/read", "{\"lastId\":999999999}"), 204);
        expect(post_(bass, url, "{\"body\":\"다음 합주 언제 해요?\"}"), 200);
        assertThat(unread(host, bandId)).isEqualTo(1);
    }

    @Test
    void 이전_메시지는_id_커서로_나눠_읽는다() throws Exception {
        Person host = person("페이지방장", "[\"GUITAR\"]");
        Person bass = person("페이지베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "페이지 밴드");
        String url = "/api/bands/" + bandId + "/messages";
        for (int i = 1; i <= 34; i++) expect(post_(i % 2 == 0 ? host : bass, url, "{\"body\":\"메시지 " + i + "\"}"), 200);

        JsonNode first = read(get_(host, url));   // 안내 1 + 34 = 35개 중 최신 30개
        assertThat(first.get("messages")).hasSize(30);
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(first.get("messages").get(29).get("body").asText()).isEqualTo("메시지 34");

        long oldest = first.get("messages").get(0).get("id").asLong();
        JsonNode rest = read(get_(host, url + "?before=" + oldest));
        assertThat(rest.get("messages")).hasSize(5);
        assertThat(rest.get("hasMore").asBoolean()).isFalse();
        assertThat(rest.get("messages").get(0).get("type").asText()).isEqualTo("SYSTEM");
    }

    @Test
    void 멤버가_아니면_볼_수도_쓸_수도_없다() throws Exception {
        Person host = person("막는방장", "[\"GUITAR\"]");
        Person bass = person("막는베이스", "[\"BASS\"]");
        Person other = person("지나가는사람", "[\"DRUM\"]");
        long bandId = bandWithMember(host, bass, "막는 밴드");
        String url = "/api/bands/" + bandId + "/messages";

        get_(other, url).andExpect(status().isForbidden());
        post_(other, url, "{\"body\":\"hi\"}").andExpect(status().isForbidden());
        post_(bass, url, "{\"body\":\"   \"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("메시지를 입력해 주세요"));

        // 나간 멤버도 더 이상 못 본다
        expect(delete_(bass, "/api/bands/" + bandId + "/members/me"), 204);
        get_(bass, url).andExpect(status().isForbidden());
        get_(host, url).andExpect(jsonPath("$.messages[-1].body").value("막는베이스님이 밴드를 나갔어요"));
    }
}
