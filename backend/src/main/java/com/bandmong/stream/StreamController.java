package com.bandmong.stream;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 화면이 하나 열어 두는 실시간 연결. 채팅(message)과 알림(notification)이 이 길로 온다. */
@RestController
public class StreamController {

    private final SseHub hub;

    public StreamController(SseHub hub) {
        this.hub = hub;
    }

    @GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Long userId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no"); // nginx가 모아서 보내지 않게
        response.setHeader("Cache-Control", "no-cache");
        return hub.connect(userId);
    }
}
