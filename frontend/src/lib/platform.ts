import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

/**
 * 웹과 앱의 차이를 이 파일 하나에 모은다.
 * - 웹: 화면과 API가 같은 주소 → 세션 쿠키 + CSRF 헤더
 * - 앱: 화면은 앱 안(capacitor://localhost), API는 운영 서버 → 로그인 토큰을 Authorization 헤더로
 */
export const isApp = Capacitor.isNativePlatform();
export const platform = Capacitor.getPlatform(); // 'ios' | 'android' | 'web'

/** 앱에서 부를 서버 주소. 웹은 빈 문자열(같은 주소). */
export const apiBase: string = isApp ? (import.meta.env.VITE_API_BASE ?? '').replace(/\/$/, '') : '';

const KEY = 'bandmong.token';
let token: string | null = null;

/** 앱 시작 때 한 번: 저장해 둔 로그인 토큰을 메모리로 */
export async function loadToken(): Promise<string | null> {
  if (!isApp) return null;
  try {
    token = (await Preferences.get({ key: KEY })).value;
  } catch {
    token = null;
  }
  return token;
}

export const getToken = () => token;

export async function setToken(next: string | null) {
  token = next;
  if (!isApp) return;
  try {
    if (next) await Preferences.set({ key: KEY, value: next });
    else await Preferences.remove({ key: KEY });
  } catch {
    // 저장 실패: 이번 실행 동안은 메모리 토큰으로 버틴다
  }
}
