import { api } from './client';
import type { MessagePage, MessageView } from './types';

export const getMessages = (bandId: number, before?: number) =>
  api<MessagePage>(`/api/bands/${bandId}/messages${before ? `?before=${before}` : ''}`);
export const sendMessage = (bandId: number, body: string, clientId: string) =>
  api<MessageView>(`/api/bands/${bandId}/messages`, { method: 'POST', body: JSON.stringify({ body, clientId }) });
export const markRead = (bandId: number, lastId: number) =>
  api<void>(`/api/bands/${bandId}/messages/read`, { method: 'POST', body: JSON.stringify({ lastId }) });

/** 다시 보내도 서버가 한 번만 저장하도록 메시지마다 붙이는 id */
export function newClientId(): string {
  const c = globalThis.crypto as Crypto | undefined;
  if (c?.randomUUID) return c.randomUUID();
  return 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 12);
}
