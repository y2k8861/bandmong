package com.bandmong.studio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookingRequest(
        @NotNull(message = "방을 골라 주세요") Long roomId,
        @NotNull(message = "날짜를 골라 주세요") LocalDate date,
        /** 그 날짜 기준 시작 시(24 이상이면 다음 날 새벽) */
        @Min(value = 0, message = "시작 시간이 올바르지 않아요") @Max(value = 29, message = "시작 시간이 올바르지 않아요") int startHour,
        @Min(value = 1, message = "1시간 이상 예약해 주세요") @Max(value = 4, message = "한 번에 4시간까지 예약할 수 있어요") int hours,
        /** 이 예약이 어느 합주를 위한 것인지 (선택) */
        Long jamId
) {
}
