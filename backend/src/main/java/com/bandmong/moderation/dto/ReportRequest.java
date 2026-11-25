package com.bandmong.moderation.dto;

import com.bandmong.moderation.ReportReason;
import com.bandmong.moderation.ReportTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull(message = "무엇을 신고하는지 알려 주세요") ReportTarget targetType,
        @NotNull(message = "무엇을 신고하는지 알려 주세요") Long targetId,
        @NotNull(message = "신고 이유를 골라 주세요") ReportReason reason,
        @Size(max = 500, message = "500자 이내로 적어 주세요") String detail
) {
}
