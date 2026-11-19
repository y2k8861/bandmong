package com.bandmong.band.dto;

import com.bandmong.band.Genre;
import com.bandmong.jam.dto.JamBrief;
import com.bandmong.user.Instrument;
import com.bandmong.user.Level;

import java.util.List;

/** 찾기 목록·내 밴드 목록 카드 한 장 */
public record BandSummary(
        Long id,
        String title,
        Genre genre,
        Level level,
        String area,
        String schedule,
        boolean recruiting,
        String hostNickname,
        int memberCount,
        List<SeatView> seats,
        MyRelation me,
        Instrument myInstrument,
        /** 방장에게만: 답을 기다리는 지원 수 */
        Integer pendingCount,
        /** 멤버에게만: 다음 합주 */
        JamBrief nextJam
) {
}
