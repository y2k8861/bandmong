package com.bandmong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 시간은 DB에 UTC(Instant)로 저장하고, "이번 주"·"요일" 같은 달력 판단만 한국 시간으로 한다.
 * Clock을 빈으로 두면 테스트에서 "지금"을 고정할 수 있다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
