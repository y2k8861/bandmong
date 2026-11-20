package com.bandmong.studio.dto;

import com.bandmong.studio.BookingStatus;

import java.time.Instant;
import java.util.List;

public record BookingView(
        Long id, Long bandId, String studioName, String roomName, String area, String station,
        Instant startsAt, Instant endsAt, int hours, int price, int perPerson, BookingStatus status, Long jamId,
        boolean host, List<SettleView> settles, int paidCount, SettleView mine
) {
    public record SettleView(Long userId, String nickname, int amount, boolean paid) {
    }
}
