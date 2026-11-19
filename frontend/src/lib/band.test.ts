import { describe, expect, it } from 'vitest';
import { ago, openCount, seatError } from './band';

describe('자리 규칙 (서버와 같은 문구)', () => {
  it('악기당 3자리, 전체 8자리', () => {
    expect(seatError({ BASS: 4 })).toBe('한 악기는 3자리까지 만들 수 있어요');
    expect(seatError({ VOCAL: 3, GUITAR: 3, BASS: 3 })).toBe('자리는 모두 8개까지 만들 수 있어요');
    expect(seatError({ VOCAL: 1, GUITAR: 2 })).toBeNull();
  });
  it('멤버가 있는 자리는 줄일 수 없다', () => {
    expect(seatError({ BASS: 0 }, { BASS: 1 })).toBe('이미 멤버가 있는 자리는 줄일 수 없어요');
  });
  it('빈자리 수', () => {
    const seats = [{ instrument: 'BASS' as const, total: 2, filled: 1 }];
    expect(openCount(seats, 'BASS')).toBe(1);
    expect(openCount(seats, 'DRUM')).toBe(0);
  });
});

describe('지난 시간', () => {
  const now = new Date('2026-11-19T12:00:00Z').getTime();
  it('짧게 표시', () => {
    expect(ago('2026-11-19T11:59:30Z', now)).toBe('방금');
    expect(ago('2026-11-19T11:45:00Z', now)).toBe('15분 전');
    expect(ago('2026-11-19T09:00:00Z', now)).toBe('3시간 전');
    expect(ago('2026-11-17T12:00:00Z', now)).toBe('2일 전');
  });
});
