package com.bandmong.jam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/** 이 주에 내가 가능한 칸 전체 (보낸 목록으로 통째로 바꾼다) */
public record AvailabilityRequest(
        @NotNull(message = "어느 주인지 알려 주세요")
        LocalDate weekStart,

        @NotNull(message = "가능한 시간을 보내 주세요")
        @Size(max = 42, message = "고를 수 있는 칸보다 많아요")
        Set<Integer> slots
) {
}
