package com.bandmong.band.dto;

import com.bandmong.user.Instrument;

public record SeatView(Instrument instrument, int total, int filled) {
    public int open() {
        return Math.max(0, total - filled);
    }
}
