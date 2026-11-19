package com.bandmong.jam.dto;

import com.bandmong.jam.RsvpAnswer;
import com.bandmong.user.Instrument;

/** 멤버 한 명의 참석 응답. answer가 null이면 아직 답하지 않음. */
public record RsvpView(Long userId, String nickname, Instrument instrument, RsvpAnswer answer) {
}
