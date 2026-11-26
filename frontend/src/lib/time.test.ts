import { describe, expect, it } from 'vitest';
import { addDays, dday, fmtDateTime, slotIso, thisMonday, thisMonthKst, toKstInput, todayKst } from './time';

describe('한국 시간 도우미', () => {
  it('이번 주 월요일은 한국 날짜 기준', () => {
    // 2026-11-22(일) 23:30 KST = 14:30 UTC
    expect(thisMonday(new Date('2026-11-22T14:30:00Z'))).toBe('2026-11-16');
    // 2026-11-23(월) 00:30 KST = 11-22 15:30 UTC → 새 주
    expect(thisMonday(new Date('2026-11-22T15:30:00Z'))).toBe('2026-11-23');
  });
  it('오늘·이번 달은 한국 날짜 기준', () => {
    // 2026-11-30 23:30 KST = 14:30 UTC → 아직 11월
    expect(todayKst(Date.parse('2026-11-30T14:30:00Z'))).toBe('2026-11-30');
    // 2026-12-01 00:30 KST = 11-30 15:30 UTC → 12월
    expect(todayKst(Date.parse('2026-11-30T15:30:00Z'))).toBe('2026-12-01');
    expect(thisMonthKst(Date.parse('2026-11-30T15:30:00Z'))).toBe('2026-12');
  });
  it('칸 → 시작 시각', () => {
    expect(slotIso('2026-11-23', 4 * 24 + 20)).toBe('2026-11-27T20:00:00+09:00');
    expect(addDays('2026-12-30', 3)).toBe('2027-01-02');
  });
  it('표시', () => {
    expect(fmtDateTime('2026-11-27T11:00:00Z')).toBe('11월 27일 (금) 20:00');
    expect(toKstInput('2026-11-27T11:00:00Z')).toEqual({ date: '2026-11-27', time: '20:00' });
    expect(dday('2026-11-27T11:00:00Z', new Date('2026-11-24T01:00:00Z'))).toBe('D-3');
    expect(dday('2026-11-27T11:00:00Z', new Date('2026-11-27T01:00:00Z'))).toBe('D-DAY');
  });
});
