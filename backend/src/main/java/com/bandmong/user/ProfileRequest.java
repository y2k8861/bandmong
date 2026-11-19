package com.bandmong.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ProfileRequest(
        @NotNull(message = "닉네임을 입력해 주세요")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]{2,10}$", message = "닉네임은 한글·영문·숫자 2~10자예요")
        String nickname,

        @NotEmpty(message = "악기를 하나 이상 골라 주세요")
        Set<Instrument> instruments,

        @NotNull(message = "실력을 골라 주세요")
        Level level,

        @Size(max = 30, message = "지역은 30자 이내로 적어 주세요")
        String region
) {
}
