import { api } from './client';
import type { CueItem, GigView } from './types';

export const listGigs = (bandId: number) => api<GigView[]>(`/api/bands/${bandId}/gigs`);
export const createGig = (bandId: number, g: { title: string; startsAt: string; venue: string; slotMinutes: number }) =>
  api<GigView>(`/api/bands/${bandId}/gigs`, { method: 'POST', body: JSON.stringify(g) });
export const getGig = (id: number) => api<GigView>(`/api/gigs/${id}`);
export const cancelGig = (id: number) => api<GigView>(`/api/gigs/${id}/cancel`, { method: 'POST' });
export const saveCue = (id: number, version: number, items: CueItem[]) =>
  api<GigView>(`/api/gigs/${id}/items`, {
    method: 'PUT',
    body: JSON.stringify({ version, items: items.map(({ type, title, durationSec }) => ({ type, title, durationSec })) }),
  });
export const addCheck = (id: number, text: string) =>
  api<GigView>(`/api/gigs/${id}/checks`, { method: 'POST', body: JSON.stringify({ text }) });
export const markCheck = (id: number, checkId: number, done: boolean) =>
  api<GigView>(`/api/gigs/${id}/checks/${checkId}`, { method: 'PUT', body: JSON.stringify({ done }) });
export const deleteCheck = (id: number, checkId: number) => api<GigView>(`/api/gigs/${id}/checks/${checkId}`, { method: 'DELETE' });
