package com.bandmong.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인한 사람마다 열린 SSE 연결(탭·기기마다 하나)을 들고 있다가 이벤트를 밀어 준다.
 *
 * 한계: 연결 목록이 서버 메모리에 있어서 서버가 한 대일 때만 맞다.
 * 서버를 여러 대로 늘리면 Redis pub/sub 같은 걸로 "다른 서버에 붙은 사람"에게도 전달해야 한다.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> byUser = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        // 연결 직후 한 번 보내야 프록시가 응답 헤더를 바로 넘겨준다
        deliver(userId, emitter, SseEmitter.event().name("ready").data(Map.of("ok", true), MediaType.APPLICATION_JSON));
        return emitter;
    }

    public void send(Long userId, String event, Object data) {
        Set<SseEmitter> set = byUser.get(userId);
        if (set == null) return;
        for (SseEmitter e : set) {
            deliver(userId, e, SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        }
    }

    public void sendAll(Collection<Long> userIds, String event, Object data) {
        userIds.forEach(id -> send(id, event, data));
    }

    /** 끊긴 연결을 찾아내고, 중간 프록시가 조용한 연결을 끊지 않게 25초마다 주석 한 줄 */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        byUser.forEach((userId, set) -> set.forEach(e -> deliver(userId, e, SseEmitter.event().comment("ping"))));
    }

    public int connections(Long userId) {
        Set<SseEmitter> set = byUser.get(userId);
        return set == null ? 0 : set.size();
    }

    private void deliver(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 연결 정리 user={} ({})", userId, e.getMessage());
            remove(userId, emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 이미 닫힘
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        byUser.computeIfPresent(userId, (k, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }
}
