package com.bandmong.band.dto;

import com.bandmong.band.BandMember;
import com.bandmong.band.MemberRole;
import com.bandmong.user.Instrument;
import com.bandmong.user.Level;

public record MemberView(Long userId, String nickname, Instrument instrument, MemberRole role, Level level) {
    public static MemberView of(BandMember m) {
        return new MemberView(m.getUser().getId(), m.getUser().getNickname(), m.getInstrument(), m.getRole(),
                m.getUser().getLevel());
    }
}
