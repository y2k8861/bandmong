package com.bandmong.band.dto;

import com.bandmong.user.Instrument;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @NotNull(message = "어떤 악기로 지원할지 골라 주세요")
        Instrument instrument,

        @Size(max = 200, message = "한마디는 200자 이내로 적어 주세요")
        String message
) {
}
