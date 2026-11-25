import { api } from './client';
import type { BandDetail, ModerationAction, ReportReason, ReportTarget, ReportView } from './types';

export const report = (targetType: ReportTarget, targetId: number, reason: ReportReason, detail: string) =>
  api<{ id: number; message: string }>('/api/reports', { method: 'POST', body: JSON.stringify({ targetType, targetId, reason, detail }) });

export const adminReports = (status: 'OPEN' | 'RESOLVED' | 'DISMISSED') => api<ReportView[]>(`/api/admin/reports?status=${status}`);
export const resolveReport = (id: number, action: ModerationAction, note: string) =>
  api<ReportView>(`/api/admin/reports/${id}/resolve`, { method: 'POST', body: JSON.stringify({ action, note }) });
export const liftRestriction = (userId: number) => api<{ lifted: number }>(`/api/admin/users/${userId}/lift`, { method: 'POST' });

export const transferHost = (bandId: number, userId: number) =>
  api<BandDetail>(`/api/bands/${bandId}/host`, { method: 'POST', body: JSON.stringify({ userId }) });
export const withdraw = () => api<void>('/api/me', { method: 'DELETE' });
