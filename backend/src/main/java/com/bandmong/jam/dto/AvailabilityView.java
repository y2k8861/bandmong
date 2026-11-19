package com.bandmong.jam.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AvailabilityView(
        LocalDate weekStart,
        /** 그리드 행: 시작 시(한국 시간) */
        List<Integer> hours,
        /** 내가 고른 칸 */
        List<Integer> mine,
        /** 칸별 가능 인원 (slot → 명) */
        Map<Integer, Integer> counts,
        int memberCount,
        List<Voter> voters,
        /** 가장 많이 겹치는 앞으로의 칸 (최대 3개) */
        List<Best> best
) {
    public record Voter(Long userId, String nickname, boolean voted) {
    }

    public record Best(int slot, Instant startsAt, int count, List<String> names) {
    }
}
