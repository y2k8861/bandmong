package com.bandmong.band.dto;

import com.bandmong.band.ApplicationStatus;
import com.bandmong.band.BandApplication;
import com.bandmong.user.Instrument;
import com.bandmong.user.Level;

import java.time.Instant;
import java.util.List;

public record ApplicationView(
        Long id,
        Long userId,
        String nickname,
        List<Instrument> userInstruments,
        Level level,
        String region,
        Instrument instrument,
        String message,
        ApplicationStatus status,
        Instant createdAt
) {
    public static ApplicationView of(BandApplication a) {
        var u = a.getUser();
        return new ApplicationView(a.getId(), u.getId(), u.getNickname(),
                u.getInstruments().stream().sorted().toList(), u.getLevel(), u.getRegion(),
                a.getInstrument(), a.getMessage(), a.getStatus(), a.getCreatedAt());
    }
}
