import type { Instrument, Level } from '../api/types';

export const INSTRUMENTS: { id: Instrument; name: string; sub: string }[] = [
  { id: 'VOCAL', name: '보컬', sub: 'Vocal' },
  { id: 'GUITAR', name: '기타', sub: 'Electric · Acoustic' },
  { id: 'BASS', name: '베이스', sub: 'Bass' },
  { id: 'DRUM', name: '드럼', sub: 'Drums' },
  { id: 'KEYS', name: '키보드', sub: 'Keys · Synth' },
  { id: 'ETC', name: '기타 악기', sub: '관악 · 현악 · 퍼커션' },
];

export const LEVELS: { id: Level; name: string }[] = [
  { id: 'BEGINNER', name: '입문' },
  { id: 'HOBBY', name: '취미 꾸준히' },
  { id: 'STAGE', name: '공연 경험' },
];

export const instrumentName = (id: Instrument) => INSTRUMENTS.find((i) => i.id === id)?.name ?? id;

/** 서버와 같은 규칙. 화면에서 먼저 막아 주고, 최종 판정은 서버가 한다. */
export function nicknameError(nick: string): string | null {
  const v = nick.trim();
  if (!v) return '닉네임을 입력해 주세요';
  if (!/^[가-힣A-Za-z0-9]{2,10}$/.test(v)) return '닉네임은 한글·영문·숫자 2~10자예요';
  return null;
}
