package com.bandmong.band.dto;

import com.bandmong.band.Genre;
import com.bandmong.jam.dto.JamBrief;
import com.bandmong.user.Level;

import java.time.Instant;
import java.util.List;

public record BandDetail(
        Long id,
        String title,
        Genre genre,
        Level level,
        String area,
        String schedule,
        String intro,
        boolean recruiting,
        Instant createdAt,
        List<SeatView> seats,
        List<MemberView> members,
        MyRelation me,
        /** 내가 낸 대기 중 지원 (있을 때만) */
        ApplicationView myApplication,
        /** 방장에게만: 대기 중 지원 목록 */
        List<ApplicationView> applications,
        /** 멤버에게만: 다음 합주 */
        JamBrief nextJam
) {
}
