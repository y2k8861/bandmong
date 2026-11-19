package com.bandmong.band.dto;

import com.bandmong.user.Instrument;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** 방장 설정. 보낸 값만 바꾼다. */
public record BandUpdateRequest(
        @Size(max = 300, message = "소개는 300자 이내로 적어 주세요")
        String intro,

        @Size(max = 20, message = "정기 모임은 20자 이내로 적어 주세요")
        String schedule,

        Boolean recruiting,

        Map<Instrument, Integer> seats
) {
}
