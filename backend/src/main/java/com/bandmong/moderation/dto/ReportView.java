package com.bandmong.moderation.dto;

import com.bandmong.moderation.ReportReason;
import com.bandmong.moderation.ReportStatus;
import com.bandmong.moderation.ReportTarget;

import java.time.Instant;

/** 관리자 화면 한 줄 */
public record ReportView(
        Long id, ReportTarget targetType, Long targetId, ReportReason reason, String reasonLabel, String detail,
        ReportStatus status, Instant createdAt, String reporterNickname,
        Long accusedId, String accusedNickname,
        /** 신고 대상 요약: 메시지 내용 / 밴드 이름 / 회원 닉네임 */
        String targetSummary,
        /** 같은 대상에 열린 신고 수 */
        long openOnTarget,
        /** 이 사람이 지금까지 받은 신고 수 */
        long reportsAgainstAccused,
        Instant accusedRestrictedUntil,
        String resolution, String adminNote
) {
}
