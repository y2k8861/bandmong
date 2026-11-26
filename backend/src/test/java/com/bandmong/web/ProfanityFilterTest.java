package com.bandmong.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    void 기본_욕설과_변형을_잡는다() {
        assertThat(filter.contains("아 씨발 진짜")).isTrue();
        assertThat(filter.contains("씨 발")).isTrue();        // 한 글자씩 띄움
        assertThat(filter.contains("씨.발")).isTrue();        // 기호
        assertThat(filter.contains("씨1발")).isTrue();        // 숫자
        assertThat(filter.contains("ㅅㅂ 늦었네")).isTrue();
        assertThat(filter.contains("What the FUCK")).isTrue();
    }

    @Test
    void 평범한_말은_잡지_않는다() {
        assertThat(filter.contains("다시 발표해 볼게요")).isFalse(); // 띄어쓰기 경계
        assertThat(filter.contains("여기가 시발점이에요")).isFalse(); // 허용 낱말
        assertThat(filter.contains("우리 집 시바견")).isFalse();
        assertThat(filter.contains("금요일 밤 록 커버")).isFalse();
        assertThat(filter.contains(null)).isFalse();
    }

    @Test
    void 걸린_글자만_가린다() {
        assertThat(filter.mask("아 씨발 진짜 늦었네")).isEqualTo("아 ** 진짜 늦었네");
        assertThat(filter.mask("씨 발 왜 늦어")).isEqualTo("* * 왜 늦어");
        assertThat(filter.mask("좋아요 👍")).isEqualTo("좋아요 👍");
    }
}
