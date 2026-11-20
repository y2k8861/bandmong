import type { CueItem } from '../api/types';

/** 3:45 */
export const mmss = (sec: number) => `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;

/** 공연 시작 시각(ms)부터 줄마다 시작 시각(한국 HH:MM)을 매긴다 */
export function cueTimes(startsAt: string, items: CueItem[]): string[] {
  let acc = 0;
  const base = new Date(startsAt).getTime() + 9 * 3600_000;
  return items.map((it) => {
    const d = new Date(base + acc * 1000);
    acc += it.durationSec;
    return `${String(d.getUTCHours()).padStart(2, '0')}:${String(d.getUTCMinutes()).padStart(2, '0')}`;
  });
}

export const totalSec = (items: CueItem[]) => items.reduce((a, x) => a + x.durationSec, 0);

export function move<T>(list: T[], i: number, d: number): T[] {
  const j = i + d;
  if (j < 0 || j >= list.length) return list;
  const next = [...list];
  [next[i], next[j]] = [next[j], next[i]];
  return next;
}
