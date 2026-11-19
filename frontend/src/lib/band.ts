import type { Genre, Instrument, SeatView, Seats } from '../api/types';

export const GENRES: { id: Genre; name: string }[] = [
  { id: 'ROCK', name: '록' },
  { id: 'CITYPOP', name: '시티팝' },
  { id: 'INDIE', name: '인디' },
  { id: 'JAZZ', name: '재즈' },
  { id: 'BALLAD', name: '발라드' },
];
export const genreName = (g: Genre) => GENRES.find((x) => x.id === g)?.name ?? g;

export const MAX_SEATS = 8;
export const MAX_PER_INSTRUMENT = 3;

export const openSeats = (seats: SeatView[]) => seats.filter((s) => s.total > s.filled);
export const openCount = (seats: SeatView[], inst: Instrument) => {
  const s = seats.find((x) => x.instrument === inst);
  return s ? Math.max(0, s.total - s.filled) : 0;
};
export const seatTotal = (seats: Seats) => Object.values(seats).reduce((a, b) => a + (b ?? 0), 0);

/** 서버와 같은 자리 규칙. 문구도 서버와 맞춘다. */
export function seatError(seats: Seats, minimum: Seats = {}): string | null {
  for (const [k, v] of Object.entries(seats) as [Instrument, number][]) {
    if (v > MAX_PER_INSTRUMENT) return `한 악기는 ${MAX_PER_INSTRUMENT}자리까지 만들 수 있어요`;
    if (v < (minimum[k] ?? 0)) return '이미 멤버가 있는 자리는 줄일 수 없어요';
  }
  if (seatTotal(seats) > MAX_SEATS) return `자리는 모두 ${MAX_SEATS}개까지 만들 수 있어요`;
  return null;
}

/** "3분 전" 같은 짧은 시간 */
export function ago(iso: string, now = Date.now()): string {
  const s = Math.max(0, Math.round((now - new Date(iso).getTime()) / 1000));
  if (s < 60) return '방금';
  if (s < 3600) return `${Math.floor(s / 60)}분 전`;
  if (s < 86400) return `${Math.floor(s / 3600)}시간 전`;
  return `${Math.floor(s / 86400)}일 전`;
}
