package com.bandmong.activity;

import com.bandmong.user.Instrument;

/**
 * 밴드·합주 쪽에서 "일어난 일"만 알린다. 알림을 누구에게 보낼지, 채팅에 뭐라고 남길지는
 * ActivityListener가 정한다. 그래서 BandService·JamService는 알림·채팅 코드를 모른다.
 */
public final class Events {

    private Events() {
    }

    public record ApplicationSubmitted(Long bandId, Long applicantId, Instrument instrument) {
    }

    public record ApplicationDecided(Long bandId, Long applicantId, Instrument instrument, boolean accepted) {
    }

    public record MemberLeft(Long bandId, Long userId, Instrument instrument) {
    }

    public enum JamChange { SCHEDULED, MOVED, EDITED, CANCELED }

    public record JamChanged(Long jamId, Long actorId, JamChange change) {
    }
}
