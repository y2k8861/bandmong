import { api } from './client';
import type { Me, ProfileInput, Providers } from './types';

export const getProviders = () => api<Providers>('/api/auth/providers');
export const devLogin = (name: string) =>
  api<Me>('/api/dev/login', { method: 'POST', body: JSON.stringify({ name }) });
export const getMe = () => api<Me>('/api/me');
export const saveProfile = (p: ProfileInput) =>
  api<Me>('/api/me/profile', { method: 'PUT', body: JSON.stringify(p) });
// 로그아웃하면 서버가 CSRF 쿠키도 지운다. 다음 로그인을 위해 새 쿠키를 바로 받아 둔다.
export async function logout() {
  try {
    await api<void>('/api/auth/logout', { method: 'POST' });
  } finally {
    await getProviders().catch(() => undefined);
  }
}
