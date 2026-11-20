package com.bandmong.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendRequest(
        @NotBlank(message = "메시지를 입력해 주세요")
        @Size(max = 1000, message = "메시지는 1000자까지 보낼 수 있어요")
        String body,

        @Pattern(regexp = "^[A-Za-z0-9-]{8,36}$", message = "잘못된 요청이에요")
        String clientId
) {
}
