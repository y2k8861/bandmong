import { api } from './client';
import type { Me } from './types';

export interface Issued { token: string; me: Me }

const deviceName = () => (typeof navigator !== 'undefined' && /iPad/.test(navigator.userAgent) ? 'iPad' : 'iPhone');

export const appleLogin = (identityToken: string) =>
  api<Issued>('/api/app/login/apple', { method: 'POST', body: JSON.stringify({ identityToken, deviceName: deviceName() }) });
export const appDevLogin = (name: string) =>
  api<Issued>('/api/app/login/dev', { method: 'POST', body: JSON.stringify({ name, deviceName: deviceName() }) });
export const exchangeCode = (code: string) =>
  api<Issued>('/api/app/login/exchange', { method: 'POST', body: JSON.stringify({ code, deviceName: deviceName() }) });
export const appLogout = (deviceToken: string | null) =>
  api<void>('/api/app/logout', { method: 'POST', body: JSON.stringify({ deviceToken }) });
export const registerDevice = (token: string, platform: 'IOS' | 'ANDROID') =>
  api<void>('/api/app/devices', { method: 'POST', body: JSON.stringify({ token, platform }) });

export interface BlockedView { userId: number; nickname: string; blockedAt: string }
export const listBlocks = () => api<BlockedView[]>('/api/blocks');
export const blockUser = (userId: number) => api<BlockedView[]>('/api/blocks', { method: 'POST', body: JSON.stringify({ userId }) });
export const unblockUser = (userId: number) => api<BlockedView[]>(`/api/blocks/${userId}`, { method: 'DELETE' });
