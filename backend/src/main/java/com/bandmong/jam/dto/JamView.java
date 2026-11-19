package com.bandmong.jam.dto;

import com.bandmong.jam.JamStatus;
import com.bandmong.jam.RsvpAnswer;

import java.time.Instant;
import java.util.List;

public record JamView(
        Long id,
        Long bandId,
        String bandTitle,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        String place,
        String memo,
        JamStatus status,
        /** 끝난 합주 */
        boolean over,
        /** 보는 사람이 방장인지 (수정·취소 버튼) */
        boolean host,
        RsvpCounts counts,
        RsvpAnswer myAnswer,
        /** 상세에서만: 멤버별 응답 */
        List<RsvpView> answers
) {
}
