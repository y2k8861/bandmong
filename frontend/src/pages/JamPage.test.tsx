import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import JamPage from './JamPage';
import type { JamView } from '../api/types';

const future = new Date(Date.now() + 3 * 86400_000).toISOString();
const end = new Date(Date.now() + 3 * 86400_000 + 7200_000).toISOString();
const jam: JamView = {
  id: 5, bandId: 3, bandTitle: '금요일 밤 록', startsAt: future, endsAt: end, durationMinutes: 120, place: '합정 A룸', memo: null,
  status: 'SCHEDULED', over: false, host: false, counts: { yes: 1, late: 0, no: 0, none: 1 }, myAnswer: null,
  answers: [
    { userId: 1, nickname: '기타방장', instrument: 'GUITAR', answer: 'YES' },
    { userId: 7, nickname: '베이시스트', instrument: 'BASS', answer: null },
  ],
};
const json = (b: unknown, status = 200) => new Response(JSON.stringify(b), { status });

function renderAt() {
  render(<MemoryRouter initialEntries={['/jams/5']}><Routes><Route path="/jams/:id" element={<JamPage />} /></Routes></MemoryRouter>);
}

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('합주 상세', () => {
  it('늦어요로 답하면 서버에 보내고 응답이 바뀐다', async () => {
    const answered: JamView = { ...jam, myAnswer: 'LATE', counts: { yes: 1, late: 1, no: 0, none: 0 },
      answers: [jam.answers![0], { ...jam.answers![1], answer: 'LATE' }] };
    const f = vi.fn().mockResolvedValueOnce(json(jam)).mockResolvedValueOnce(json(answered));
    vi.stubGlobal('fetch', f);
    renderAt();

    expect(await screen.findByText('아직 답 안 함 1')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '늦어요' }));
    expect(await screen.findByText('늦어요 1')).toBeTruthy();
    const [url, init] = f.mock.calls[1] as [string, RequestInit];
    expect(url).toBe('/api/jams/5/rsvp');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body as string)).toEqual({ answer: 'LATE' });
  });

  it('취소된 합주는 답 버튼이 없다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ ...jam, status: 'CANCELED' })));
    renderAt();
    expect(await screen.findByText('취소됨')).toBeTruthy();
    expect(screen.queryByRole('button', { name: '참석' })).toBeNull();
  });
});
