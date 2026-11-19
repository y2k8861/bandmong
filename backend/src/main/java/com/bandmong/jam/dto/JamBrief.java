package com.bandmong.jam.dto;

import com.bandmong.jam.RsvpAnswer;

import java.time.Instant;

/** 밴드 카드·상세에 붙는 "다음 합주" 한 줄 */
public record JamBrief(Long id, Instant startsAt, int durationMinutes, String place, RsvpAnswer myAnswer) {
}
