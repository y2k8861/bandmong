package com.bandmong.band.dto;

import com.bandmong.band.Genre;
import com.bandmong.user.Instrument;
import com.bandmong.user.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record BandCreateRequest(
        @NotBlank(message = "밴드 이름을 입력해 주세요")
        @Size(max = 24, message = "밴드 이름은 24자 이내로 적어 주세요")
        String title,

        @NotNull(message = "장르를 골라 주세요")
        Genre genre,

        /** null이면 실력 상관없음 */
        Level level,

        @NotBlank(message = "지역을 입력해 주세요")
        @Size(max = 20, message = "지역은 20자 이내로 적어 주세요")
        String area,

        @Size(max = 20, message = "정기 모임은 20자 이내로 적어 주세요")
        String schedule,

        @Size(max = 300, message = "소개는 300자 이내로 적어 주세요")
        String intro,

        @NotNull(message = "내가 맡을 악기를 골라 주세요")
        Instrument hostInstrument,

        @NotEmpty(message = "필요한 악기 자리를 정해 주세요")
        Map<Instrument, Integer> seats
) {
}
