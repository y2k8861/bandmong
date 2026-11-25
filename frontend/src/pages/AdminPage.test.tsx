import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminPage from './AdminPage';
import ReportSheet from '../components/ReportSheet';
import type { ReportView } from '../api/types';

const json = (b: unknown, status = 200) => new Response(JSON.stringify(b), { status });
const base: ReportView = {
  id: 1, targetType: 'MESSAGE', targetId: 50, reason: 'ABUSE', reasonLabel: '욕설·비하', detail: '욕설', status: 'OPEN',
  createdAt: new Date().toISOString(), reporterNickname: '신고자', accusedId: 9, accusedNickname: '나쁜말',
  targetSummary: '아주 나쁜 말', openOnTarget: 2, reportsAgainstAccused: 3, accusedRestrictedUntil: null, resolution: null, adminNote: null,
};
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('관리자 신고 처리', () => {
  it('메시지 신고에는 가리기가 있고, 처리하면 같은 대상 신고가 목록에서 함께 빠진다', async () => {
    const f = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'POST') return json({ ...base, status: 'RESOLVED' });
      return json([base, { ...base, id: 2, reporterNickname: '다른신고자' }, { ...base, id: 3, targetType: 'BAND', targetId: 7, targetSummary: '수상한 밴드' }]);
    });
    vi.stubGlobal('fetch', f);
    render(<MemoryRouter><AdminPage /></MemoryRouter>);

    expect(await screen.findAllByText('아주 나쁜 말')).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: '메시지 가리기' })).toHaveLength(2);
    expect(screen.getByRole('button', { name: '모집 닫기' })).toBeTruthy(); // 밴드 신고
    fireEvent.click(screen.getAllByRole('button', { name: '메시지 가리기' })[0]);

    await waitFor(() => expect(screen.queryByText('아주 나쁜 말')).toBeNull());
    expect(screen.getByText('수상한 밴드')).toBeTruthy();
    const post = f.mock.calls.find((c) => (c[1] as RequestInit | undefined)?.method === 'POST')!;
    expect(post[0]).toBe('/api/admin/reports/1/resolve');
    expect(JSON.parse((post[1] as RequestInit).body as string)).toEqual({ action: 'HIDE_MESSAGE', note: '' });
  });
});

describe('신고 시트', () => {
  it('이유를 골라야 보낼 수 있고, 보내면 안내 문구를 보여준다', async () => {
    const f = vi.fn(async () => json({ id: 1, message: '신고했어요. 운영팀이 확인하고 결과를 알려 드릴게요' }, 201));
    vi.stubGlobal('fetch', f);
    render(<ReportSheet target="USER" id={9} label="나쁜말님" onClose={() => undefined} />);
    const send = screen.getByRole('button', { name: '신고하기' }) as HTMLButtonElement;
    expect(send.disabled).toBe(true);
    fireEvent.click(screen.getByLabelText('합주 노쇼·잠수'));
    fireEvent.click(send);
    expect(await screen.findByRole('status')).toHaveProperty('textContent', '신고했어요. 운영팀이 확인하고 결과를 알려 드릴게요');
    expect(JSON.parse(((f.mock.calls[0] as unknown[])[1] as RequestInit).body as string))
      .toEqual({ targetType: 'USER', targetId: 9, reason: 'NO_SHOW', detail: '' });
  });
});
