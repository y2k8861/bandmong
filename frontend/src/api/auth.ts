import { api } from './client';
import { appLogout } from './app';
import { isApp, setToken } from '../lib/platform';
import { currentPushToken } from '../lib/native';
import type { Me, ProfileInput, Providers } from './types';

export const getProviders = () => api<Providers>('/api/auth/providers');
export const devLogin = (name: string) =>
  api<Me>('/api/dev/login', { method: 'POST', body: JSON.stringify({ name }) });
export const getMe = () => api<Me>('/api/me');
export const saveProfile = (p: ProfileInput) =>
  api<Me>('/api/me/profile', { method: 'PUT', body: JSON.stringify(p) });
// 로그아웃하면 서버가 CSRF 쿠키도 지운다. 다음 로그인을 위해 새 쿠키를 바로 받아 둔다.
export async function logout() {
  if (isApp) {
    // 앱: 서버에서 이 기기 토큰을 끊고, 푸시도 이 계정으로 안 오게
    try {
      await appLogout(currentPushToken());
    } finally {
      await setToken(null);
    }
    return;
  }
  try {
    await api<void>('/api/auth/logout', { method: 'POST' });
  } finally {
    await getProviders().catch(() => undefined);
  }
}
