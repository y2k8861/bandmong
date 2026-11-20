package com.bandmong.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReadRequest(@NotNull @PositiveOrZero Long lastId) {
}
