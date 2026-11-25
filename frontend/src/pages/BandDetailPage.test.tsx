import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BandDetailPage from './BandDetailPage';
import type { BandDetail, Me } from '../api/types';

const me: Me = { id: 7, provider: 'dev', nickname: '베이시스트', instruments: ['BASS'], level: 'HOBBY', region: null, onboarded: true, role: 'USER', restrictedUntil: null };

const base: BandDetail = {
  id: 3, title: '금요일 밤 록', genre: 'ROCK', level: null, area: '강남역', schedule: '매주 금 20:00', intro: null,
  recruiting: true, createdAt: '2026-11-19T00:00:00Z',
  seats: [{ instrument: 'GUITAR', total: 1, filled: 1 }, { instrument: 'BASS', total: 1, filled: 0 }],
  members: [{ userId: 1, nickname: '기타방장', instrument: 'GUITAR', role: 'HOST', level: 'STAGE' }],
  me: 'NONE', myApplication: null, applications: null, nextJam: null, unreadMessages: null,
};

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

function renderAt() {
  return render(
    <MemoryRouter initialEntries={['/bands/3']}>
      <Routes><Route path="/bands/:id" element={<BandDetailPage me={me} />} /></Routes>
    </MemoryRouter>,
  );
}

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('밴드 상세', () => {
  it('빈 베이스 자리에 지원하면 지원 대기 상태로 바뀐다', async () => {
    const pending: BandDetail = {
      ...base, me: 'PENDING',
      myApplication: { id: 9, userId: 7, nickname: '베이시스트', userInstruments: ['BASS'], level: 'HOBBY', region: null,
        instrument: 'BASS', message: '주 1회 가능', status: 'PENDING', createdAt: '2026-11-19T00:00:00Z' },
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json(base))
      .mockResolvedValueOnce(json(pending));
    vi.stubGlobal('fetch', fetchMock);
    renderAt();

    fireEvent.click(await screen.findByRole('button', { name: '지원하기' }));
    fireEvent.change(screen.getByLabelText(/방장에게 한마디/), { target: { value: '주 1회 가능' } });
    fireEvent.click(screen.getByRole('button', { name: '지원 보내기' }));

    expect(await screen.findByText('베이스 자리로 지원했어요')).toBeTruthy();
    const [url, init] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(url).toBe('/api/bands/3/applications');
    expect(JSON.parse(init.body as string)).toEqual({ instrument: 'BASS', message: '주 1회 가능' });
  });

  it('내 악기 자리가 없으면 지원 버튼이 꺼져 있다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ ...base, seats: [{ instrument: 'GUITAR', total: 1, filled: 1 }] })));
    renderAt();
    const btn = (await screen.findByRole('button', { name: '내 악기 자리가 없어요' })) as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
  });

  it('방장: 자리가 찬 지원자는 수락할 수 없고, 서버 거절 문구를 보여준다', async () => {
    const host: BandDetail = {
      ...base, me: 'HOST',
      seats: [{ instrument: 'GUITAR', total: 1, filled: 1 }, { instrument: 'BASS', total: 1, filled: 0 }, { instrument: 'DRUM', total: 1, filled: 1 }],
      applications: [
        { id: 21, userId: 8, nickname: '베이스에이', userInstruments: ['BASS'], level: 'HOBBY', region: '마포구', instrument: 'BASS', message: null, status: 'PENDING', createdAt: '2026-11-19T00:00:00Z' },
        { id: 22, userId: 9, nickname: '드럼비', userInstruments: ['DRUM'], level: 'BEGINNER', region: null, instrument: 'DRUM', message: null, status: 'PENDING', createdAt: '2026-11-19T00:00:00Z' },
      ],
    };
    // 주소별로 답하는 가짜 서버 (방장 화면은 합주 목록도 함께 불러온다)
    const fetchMock = vi.fn(async (url: string) => {
      if (url === '/api/bands/3/jams') return json({ upcoming: [], past: [] });
      if (url === '/api/bands/3/bookings') return json([]);
      if (url === '/api/bands/3/gigs') return json([]);
      if (url.startsWith('/api/bands/3/dues')) return json({ month: '2026-11', amount: null, account: null, host: true, paidCount: 0, total: 1, members: [] });
      if (url.endsWith('/accept')) return json({ message: '베이스 빈자리가 방금 찼어요. 자리를 늘린 뒤 수락해 주세요' }, 409);
      return json(host);
    });
    vi.stubGlobal('fetch', fetchMock);
    renderAt();

    const accepts = (await screen.findAllByRole('button', { name: '수락' })) as HTMLButtonElement[];
    expect(accepts[0].disabled).toBe(false); // 베이스: 빈자리 있음
    expect(accepts[1].disabled).toBe(true);  // 드럼: 다 참
    expect(screen.getByText('드럼 자리가 다 찼어요. 자리를 늘려야 수락할 수 있어요.')).toBeTruthy();

    fireEvent.click(accepts[0]);
    expect(await screen.findByRole('status')).toHaveProperty('textContent', '베이스 빈자리가 방금 찼어요. 자리를 늘린 뒤 수락해 주세요');
    const urls = () => fetchMock.mock.calls.map((c) => c[0] as string);
    expect(urls()).toContain('/api/bands/3/applications/21/accept');
    // 실패 후 최신 상태를 다시 읽는다
    await waitFor(() => expect(urls().filter((u) => u === '/api/bands/3').length).toBe(2));
  });
});
