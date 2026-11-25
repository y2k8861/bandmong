package com.bandmong.user;

import java.util.List;

public record MeResponse(
        Long id,
        String provider,
        String nickname,
        List<Instrument> instruments,
        Level level,
        String region,
        boolean onboarded,
        Role role,
        /** 이용 정지 중이면 풀리는 시각 (읽기만 가능) */
        java.time.Instant restrictedUntil
) {
    public static MeResponse of(User u) {
        return of(u, null);
    }

    public static MeResponse of(User u, java.time.Instant restrictedUntil) {
        return new MeResponse(u.getId(), u.getProvider(), u.getNickname(),
                u.getInstruments().stream().sorted().toList(),
                u.getLevel(), u.getRegion(), u.isOnboarded(), u.getRole(), restrictedUntil);
    }
}
