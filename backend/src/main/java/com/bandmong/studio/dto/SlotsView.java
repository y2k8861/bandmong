package com.bandmong.studio.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 방 하나의 하루 시간표. hour가 24 이상이면 다음 날 새벽. */
public record SlotsView(Long roomId, String studioName, String roomName, LocalDate date, List<Slot> slots) {
    public record Slot(int hour, Instant startsAt, int price, boolean taken, boolean past) {
    }
}
