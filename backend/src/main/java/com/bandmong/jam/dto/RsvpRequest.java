package com.bandmong.jam.dto;

import com.bandmong.jam.RsvpAnswer;
import jakarta.validation.constraints.NotNull;

public record RsvpRequest(@NotNull(message = "참석 여부를 골라 주세요") RsvpAnswer answer) {
}
