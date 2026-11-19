import { api } from './client';
import type { AvailabilityView, JamInput, JamList, JamView, RsvpAnswer } from './types';

export const getAvailability = (bandId: number, week?: string) =>
  api<AvailabilityView>(`/api/bands/${bandId}/availability${week ? `?week=${week}` : ''}`);
export const saveAvailability = (bandId: number, weekStart: string, slots: number[]) =>
  api<AvailabilityView>(`/api/bands/${bandId}/availability`, { method: 'PUT', body: JSON.stringify({ weekStart, slots }) });

export const listJams = (bandId: number) => api<JamList>(`/api/bands/${bandId}/jams`);
export const createJam = (bandId: number, j: JamInput) =>
  api<JamView>(`/api/bands/${bandId}/jams`, { method: 'POST', body: JSON.stringify(j) });
export const getJam = (id: number) => api<JamView>(`/api/jams/${id}`);
export const updateJam = (id: number, patch: Partial<JamInput>) =>
  api<JamView>(`/api/jams/${id}`, { method: 'PATCH', body: JSON.stringify(patch) });
export const cancelJam = (id: number) => api<JamView>(`/api/jams/${id}/cancel`, { method: 'POST' });
export const answerJam = (id: number, answer: RsvpAnswer) =>
  api<JamView>(`/api/jams/${id}/rsvp`, { method: 'PUT', body: JSON.stringify({ answer }) });
