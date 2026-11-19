package com.bandmong.jam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record JamCreateRequest(
        /** 예: 2026-11-27T20:00:00+09:00 */
        @NotNull(message = "합주 시간을 골라 주세요")
        OffsetDateTime startsAt,

        @Min(value = 60, message = "합주는 1시간 이상으로 잡아 주세요")
        @Max(value = 240, message = "합주는 4시간까지 잡을 수 있어요")
        int durationMinutes,

        @Size(max = 40, message = "장소는 40자 이내로 적어 주세요")
        String place,

        @Size(max = 200, message = "메모는 200자 이내로 적어 주세요")
        String memo
) {
}
