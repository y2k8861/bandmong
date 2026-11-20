import { api } from './client';
import type { BookingView, DuesView, SlotsView, StudioView } from './types';

export const getStudios = () => api<StudioView[]>('/api/studios');
export const getSlots = (roomId: number, date: string) => api<SlotsView>(`/api/rooms/${roomId}/slots?date=${date}`);
export const listBookings = (bandId: number) => api<BookingView[]>(`/api/bands/${bandId}/bookings`);
export const book = (bandId: number, b: { roomId: number; date: string; startHour: number; hours: number; jamId?: number | null }) =>
  api<BookingView>(`/api/bands/${bandId}/bookings`, { method: 'POST', body: JSON.stringify(b) });
export const cancelBooking = (id: number) => api<BookingView>(`/api/bookings/${id}/cancel`, { method: 'POST' });
export const setSettle = (id: number, userId: number, paid: boolean) =>
  api<BookingView>(`/api/bookings/${id}/settles/${userId}`, { method: 'PUT', body: JSON.stringify({ paid }) });

export const getDues = (bandId: number, month?: string) => api<DuesView>(`/api/bands/${bandId}/dues${month ? `?month=${month}` : ''}`);
export const setDuesSettings = (bandId: number, amount: number, account: string) =>
  api<DuesView>(`/api/bands/${bandId}/dues/settings`, { method: 'PUT', body: JSON.stringify({ amount, account }) });
export const setDuesPaid = (bandId: number, month: string, userId: number, paid: boolean) =>
  api<DuesView>(`/api/bands/${bandId}/dues/${month}/members/${userId}`, { method: 'PUT', body: JSON.stringify({ paid }) });
