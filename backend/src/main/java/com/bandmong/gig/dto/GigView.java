package com.bandmong.gig.dto;

import com.bandmong.gig.CueType;
import com.bandmong.gig.GigStatus;

import java.time.Instant;
import java.util.List;

public record GigView(
        Long id, Long bandId, String bandTitle, String title, Instant startsAt, String venue, int slotMinutes,
        GigStatus status, long version, boolean host, int totalSec, List<Item> items, List<Check> checks, int doneCount
) {
    public record Item(Long id, CueType type, String title, int durationSec) {
    }

    public record Check(Long id, String text, boolean done, String doneBy) {
    }
}
