import { api } from './client';
import type { NotificationPage, NotificationView } from './types';

export const getNotifications = (before?: number) =>
  api<NotificationPage>(`/api/notifications${before ? `?before=${before}` : ''}`);
export const getUnread = () => api<{ unread: number }>('/api/notifications/unread');
export const readNotification = (id: number) => api<NotificationView>(`/api/notifications/${id}/read`, { method: 'POST' });
export const readAllNotifications = () => api<{ unread: number }>('/api/notifications/read-all', { method: 'POST' });
