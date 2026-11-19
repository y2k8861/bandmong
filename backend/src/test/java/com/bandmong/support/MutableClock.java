package com.bandmong.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** 테스트에서 "지금"을 앞으로 돌려 보기 위한 시계 */
public class MutableClock extends Clock {
    private volatile Instant now = Instant.now();

    public void reset() { now = Instant.now(); }
    public void advance(Duration d) { now = now.plus(d); }

    @Override public Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}
