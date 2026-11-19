package com.bandmong.user;

import java.util.List;

public record MeResponse(
        Long id,
        String provider,
        String nickname,
        List<Instrument> instruments,
        Level level,
        String region,
        boolean onboarded
) {
    public static MeResponse of(User u) {
        return new MeResponse(u.getId(), u.getProvider(), u.getNickname(),
                u.getInstruments().stream().sorted().toList(),
                u.getLevel(), u.getRegion(), u.isOnboarded());
    }
}
