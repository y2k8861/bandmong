import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import GigPage from './GigPage';
import type { GigView } from '../api/types';

const gig: GigView = {
  id: 4, bandId: 3, bandTitle: '금요일 밤 록', title: '연말 클럽 공연', startsAt: '2026-12-20T10:30:00Z', venue: '홍대 클럽 FF',
  slotMinutes: 20, status: 'SCHEDULED', version: 3, host: false, totalSec: 105,
  items: [{ id: 1, type: 'MC', title: '여는 인사', durationSec: 60 }, { id: 2, type: 'MC', title: '마무리 인사', durationSec: 45 }],
  checks: [{ id: 9, text: '셋리스트 확정', done: false, doneBy: null }], doneCount: 0,
};
const json = (b: unknown, status = 200) => new Response(JSON.stringify(b), { status });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('공연 큐시트', () => {
  it('고친 걸 버전과 함께 저장하고, 409면 최신으로 불러오기를 띄운다', async () => {
    const f = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'PUT') return json({ message: '다른 멤버가 먼저 큐시트를 고쳤어요. 최신으로 불러온 뒤 다시 고쳐 주세요' }, 409);
      return json(gig);
    });
    vi.stubGlobal('fetch', f);
    render(<MemoryRouter initialEntries={['/gigs/4']}><Routes><Route path="/gigs/:id" element={<GigPage />} /></Routes></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: '고치기' }));
    fireEvent.click(screen.getByRole('button', { name: '여는 인사 15초 늘리기' }));
    fireEvent.click(screen.getByRole('button', { name: '큐시트 저장' }));

    expect(await screen.findByRole('button', { name: /최신으로 불러오기/ })).toBeTruthy();
    const put = f.mock.calls.find((c) => (c[1] as RequestInit | undefined)?.method === 'PUT')!;
    const body = JSON.parse((put[1] as RequestInit).body as string);
    expect(body.version).toBe(3);
    expect(body.items[0]).toEqual({ type: 'MC', title: '여는 인사', durationSec: 75 });
  });
});
