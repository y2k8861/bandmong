import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OnboardingPage from './OnboardingPage';
import type { Me } from '../api/types';

const fresh: Me = { id: 1, provider: 'dev', nickname: null, instruments: [], level: null, region: null, onboarded: false, role: 'USER', restrictedUntil: null };

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('온보딩', () => {
  it('악기·실력·닉네임을 다 채워야 시작 버튼이 켜지고, 저장하면 서버 응답을 넘긴다', async () => {
    const saved: Me = { ...fresh, nickname: '퇴근베이스', instruments: ['BASS'], level: 'HOBBY', onboarded: true, role: 'USER', restrictedUntil: null };
    const fetchMock = vi.fn(async () => new Response(JSON.stringify(saved), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    const onSaved = vi.fn();

    render(<MemoryRouter><OnboardingPage me={fresh} onSaved={onSaved} /></MemoryRouter>);
    const start = screen.getByRole('button', { name: '밴드몽 시작하기' }) as HTMLButtonElement;
    expect(start.disabled).toBe(true);

    fireEvent.click(screen.getByRole('button', { name: /베이스/ }));
    fireEvent.click(screen.getByRole('button', { name: '취미 꾸준히' }));
    fireEvent.change(screen.getByLabelText(/닉네임/), { target: { value: '퇴근베이스' } });
    expect(start.disabled).toBe(false);

    fireEvent.click(start);
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/me/profile');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body as string)).toEqual({ nickname: '퇴근베이스', instruments: ['BASS'], level: 'HOBBY', region: '' });
  });

  it('서버가 거절하면 그 문구를 보여준다', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ message: '닉네임은 한글·영문·숫자 2~10자예요' }), { status: 400 })));
    render(<MemoryRouter><OnboardingPage me={{ ...fresh, nickname: '드러머', instruments: ['DRUM'], level: 'STAGE' }} onSaved={() => {}} /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: '밴드몽 시작하기' }));
    expect(await screen.findByRole('alert')).toHaveProperty('textContent', '닉네임은 한글·영문·숫자 2~10자예요');
  });
});
