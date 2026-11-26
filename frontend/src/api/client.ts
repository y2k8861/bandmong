// 스프링 API 호출.
// 웹: 세션 쿠키를 같이 보내고, 쓰기 요청에는 CSRF 헤더를 붙인다.
// 앱: 운영 서버 주소로, 로그인 토큰을 Authorization 헤더에 (쿠키·CSRF 없음).
import { apiBase, getToken, isApp } from '../lib/platform';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export function readCookie(name: string, cookie = document.cookie): string | null {
  const hit = cookie.split('; ').find((c) => c.startsWith(name + '='));
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : null;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (isApp) {
    const t = getToken();
    if (t) headers.set('Authorization', `Bearer ${t}`);
  } else if (method !== 'GET' && method !== 'HEAD') {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers.set('X-XSRF-TOKEN', token);
  }
  const res = await fetch(apiBase + path, { ...init, method, headers, credentials: isApp ? 'omit' : 'same-origin' });
  if (!res.ok) {
    let message = '잠시 후 다시 시도해 주세요';
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* 본문 없는 오류 */
    }
    if (res.status === 401) message = '로그인이 필요해요';
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
