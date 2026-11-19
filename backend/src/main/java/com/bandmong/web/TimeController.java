package com.bandmong.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** 서버 시각. 마감·투표 시간 판정은 기기 시계가 아니라 이 값을 기준으로 한다. */
@RestController
public class TimeController {
    @GetMapping("/api/time")
    public Map<String, Object> now() {
        return Map.of("now", Instant.now().toString());
    }
}
