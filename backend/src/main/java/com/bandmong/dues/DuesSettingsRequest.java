package com.bandmong.dues;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** amount가 0이거나 없으면 회비를 끈다 */
public record DuesSettingsRequest(
        @Min(value = 0, message = "회비가 올바르지 않아요") @Max(value = 1_000_000, message = "회비는 100만 원까지 정할 수 있어요")
        Integer amount,
        @Size(max = 60, message = "계좌 안내는 60자 이내로 적어 주세요")
        String account
) {
}
