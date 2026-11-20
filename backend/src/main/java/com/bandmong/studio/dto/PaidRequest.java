package com.bandmong.studio.dto;

import jakarta.validation.constraints.NotNull;

public record PaidRequest(@NotNull(message = "냈는지 알려 주세요") Boolean paid) {
}
