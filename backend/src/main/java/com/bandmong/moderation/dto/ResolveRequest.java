package com.bandmong.moderation.dto;

import com.bandmong.moderation.ModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveRequest(
        @NotNull(message = "처리 방법을 골라 주세요") ModerationAction action,
        @Size(max = 300, message = "메모는 300자 이내로 적어 주세요") String note
) {
}
