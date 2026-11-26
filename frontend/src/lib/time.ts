// 날짜·요일 판단은 모두 한국 시간(UTC+9, 서머타임 없음)으로 한다. 서버와 같은 기준.
const HOUR = 3_600_000;
const OFFSET = 9 * HOUR;

export const DAYS = ['월', '화', '수', '목', '금', '토', '일'];

const shift = (d: Date) => new Date(d.getTime() + OFFSET); // getUTC*로 읽으면 한국 시각
const pad = (n: number) => String(n).padStart(2, '0');
const ymd = (d: Date) => `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;

/** 한국 날짜 'YYYY-MM-DD' (기기 시간대와 상관없이) */
export const todayKst = (now = Date.now()): string => ymd(new Date(now + OFFSET));
/** 그 시각의 한국 날짜 'YYYY-MM-DD' */
export const kstDate = (iso: string): string => todayKst(Date.parse(iso));
/** 한국 기준 이번 달 'YYYY-MM' */
export const thisMonthKst = (now = Date.now()): string => todayKst(now).slice(0, 7);

/** 'YYYY-MM-DD' + n일 */
export function addDays(date: string, n: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return ymd(new Date(Date.UTC(y, m - 1, d + n)));
}

/** 이번 주 월요일 (한국 날짜) */
export function thisMonday(now = new Date()): string {
  const k = shift(now);
  const dow = (k.getUTCDay() + 6) % 7;
  return addDays(ymd(k), -dow);
}

/** slot = 요일*24 + 시 → 서버에 보낼 시작 시각 */
export function slotIso(week: string, slot: number): string {
  return `${addDays(week, Math.floor(slot / 24))}T${pad(slot % 24)}:00:00+09:00`;
}

export function slotTime(week: string, slot: number): number {
  return new Date(slotIso(week, slot)).getTime();
}

function parts(iso: string) {
  const k = shift(new Date(iso));
  return { y: k.getUTCFullYear(), m: k.getUTCMonth() + 1, d: k.getUTCDate(), dow: (k.getUTCDay() + 6) % 7, h: k.getUTCHours(), mi: k.getUTCMinutes() };
}

/** 11월 27일 (금) 20:00 */
export function fmtDateTime(iso: string): string {
  const p = parts(iso);
  return `${p.m}월 ${p.d}일 (${DAYS[p.dow]}) ${pad(p.h)}:${pad(p.mi)}`;
}

export function fmtTime(iso: string): string {
  const p = parts(iso);
  return `${pad(p.h)}:${pad(p.mi)}`;
}

/** 11/27 */
export function fmtShortDate(date: string): string {
  const [, m, d] = date.split('-').map(Number);
  return `${m}/${d}`;
}

/** 한국 날짜 기준 D-day */
export function dday(iso: string, now = new Date()): string {
  const a = Date.parse(ymd(shift(new Date(iso))));
  const b = Date.parse(ymd(shift(now)));
  const n = Math.round((a - b) / (24 * HOUR));
  return n <= 0 ? 'D-DAY' : `D-${n}`;
}

/** <input type=date/time> 값 ↔ ISO */
export function toKstInput(iso: string): { date: string; time: string } {
  const p = parts(iso);
  return { date: `${p.y}-${pad(p.m)}-${pad(p.d)}`, time: `${pad(p.h)}:${pad(p.mi)}` };
}
export const fromKstInput = (date: string, time: string) => `${date}T${time}:00+09:00`;

export const ANSWERS = [
  { id: 'YES', name: '참석' },
  { id: 'LATE', name: '늦어요' },
  { id: 'NO', name: '불참' },
] as const;
export const answerName = (a: string | null) => ANSWERS.find((x) => x.id === a)?.name ?? '아직 답 안 함';
