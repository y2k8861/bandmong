import { describe, expect, it } from 'vitest';
import { cueTimes, mmss, move, totalSec } from './cue';

describe('큐시트 계산', () => {
  const items = [
    { type: 'MC' as const, title: '인사', durationSec: 60 },
    { type: 'SONG' as const, title: '첫 곡', durationSec: 245 },
    { type: 'SONG' as const, title: '둘째 곡', durationSec: 200 },
  ];
  it('줄마다 시작 시각 (한국 시간)', () => {
    // 19:30 KST = 10:30 UTC
    expect(cueTimes('2026-12-20T10:30:00Z', items)).toEqual(['19:30', '19:31', '19:35']);
  });
  it('총 길이와 표시', () => {
    expect(totalSec(items)).toBe(505);
    expect(mmss(505)).toBe('8:25');
  });
  it('순서 바꾸기는 끝을 넘지 않는다', () => {
    expect(move([1, 2, 3], 0, -1)).toEqual([1, 2, 3]);
    expect(move([1, 2, 3], 1, 1)).toEqual([1, 3, 2]);
  });
});
