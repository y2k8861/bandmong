import { api } from './client';
import type { BandCreateInput, BandDetail, BandSummary, Genre, Instrument, MyBands, Seats } from './types';

export function exploreBands(f: { genre?: Genre | null; instrument?: Instrument | null; area?: string }) {
  const q = new URLSearchParams();
  if (f.genre) q.set('genre', f.genre);
  if (f.instrument) q.set('instrument', f.instrument);
  if (f.area?.trim()) q.set('area', f.area.trim());
  const qs = q.toString();
  return api<BandSummary[]>('/api/bands' + (qs ? '?' + qs : ''));
}

export const myBands = () => api<MyBands>('/api/me/bands');
export const getBand = (id: number) => api<BandDetail>(`/api/bands/${id}`);
export const createBand = (b: BandCreateInput) =>
  api<BandDetail>('/api/bands', { method: 'POST', body: JSON.stringify(b) });
export const updateBand = (id: number, patch: { intro?: string; schedule?: string; recruiting?: boolean; seats?: Seats }) =>
  api<BandDetail>(`/api/bands/${id}`, { method: 'PATCH', body: JSON.stringify(patch) });

export const applyBand = (id: number, instrument: Instrument, message: string) =>
  api<BandDetail>(`/api/bands/${id}/applications`, { method: 'POST', body: JSON.stringify({ instrument, message }) });
export const cancelApply = (id: number) => api<BandDetail>(`/api/bands/${id}/applications/mine`, { method: 'DELETE' });
export const acceptApp = (id: number, appId: number) =>
  api<BandDetail>(`/api/bands/${id}/applications/${appId}/accept`, { method: 'POST' });
export const rejectApp = (id: number, appId: number) =>
  api<BandDetail>(`/api/bands/${id}/applications/${appId}/reject`, { method: 'POST' });
export const leaveBand = (id: number) => api<void>(`/api/bands/${id}/members/me`, { method: 'DELETE' });
