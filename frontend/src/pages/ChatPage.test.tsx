import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ChatPage, { mergeMessages } from './ChatPage';
import { StreamContext, type Stream } from '../lib/stream';
import type { Me, MessageView } from '../api/types';

const me: Me = { id: 7, provider: 'dev', nickname: '베이시스트', instruments: ['BASS'], level: 'HOBBY', region: null, onboarded: true, role: 'USER', restrictedUntil: null };
const msg = (id: number, over: Partial<MessageView> = {}): MessageView => ({
  id, bandId: 3, type: 'TEXT', senderId: 1, senderNickname: '기타방장', body: `메시지 ${id}`,
  createdAt: '2026-11-20T11:00:00Z', clientId: null, ...over,
});

describe('메시지 합치기', () => {
  it('같은 id는 한 번만, 보내는 중인 내 메시지는 서버 메시지로 바꿔 끼운다', () => {
    const local = { ...msg(-1, { senderId: 7, clientId: 'abc-12345' }), state: 'sending' as const };
    const out = mergeMessages([msg(1), local], [msg(1), msg(2, { senderId: 7, clientId: 'abc-12345' })]);
    expect(out.map((m) => m.id)).toEqual([1, 2]);
    expect(out.every((m) => !('state' in m) || !m.state)).toBe(true);
  });
});

/** 직접 이벤트를 쏠 수 있는 가짜 실시간 연결 */
function fakeStream() {
  const subs = new Map<string, Set<(d: unknown) => void>>();
  const stream: Stream = {
    connected: true, unread: 0, setUnread: () => undefined,
    subscribe: (event, fn) => {
      const set = subs.get(event) ?? new Set();
      set.add(fn as (d: unknown) => void);
      subs.set(event, set);
      return () => { set.delete(fn as (d: unknown) => void); };
    },
  };
  const emit = (event: string, data: unknown) => subs.get(event)?.forEach((fn) => fn(data));
  return { stream, emit };
}

const json = (b: unknown, status = 200) => new Response(b === undefined ? null : JSON.stringify(b), { status });

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('채팅 화면', () => {
  it('보내면 바로 보이고, 실시간으로 온 남의 메시지도 붙는다. 읽음 위치를 알린다', async () => {
    const calls: { url: string; init?: RequestInit }[] = [];
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      calls.push({ url, init });
      if (url === '/api/bands/3') return json({ title: '금요일 밤 록', members: [{}, {}] });
      if (url === '/api/bands/3/messages' && init?.method === 'POST') {
        const b = JSON.parse(init.body as string);
        return json(msg(11, { senderId: 7, senderNickname: '베이시스트', body: b.body, clientId: b.clientId }));
      }
      if (url === '/api/bands/3/messages') return json({ messages: [msg(10, { type: 'SYSTEM', senderId: null, body: '베이시스트님이 베이스로 들어왔어요' })], hasMore: false, lastReadId: 9 });
      if (url.endsWith('/read')) return json(undefined, 204);
      return json({}, 404);
    }));
    const { stream, emit } = fakeStream();
    render(
      <StreamContext.Provider value={stream}>
        <MemoryRouter initialEntries={['/bands/3/chat']}>
          <Routes><Route path="/bands/:id/chat" element={<ChatPage me={me} />} /></Routes>
        </MemoryRouter>
      </StreamContext.Provider>,
    );

    expect(await screen.findByText('베이시스트님이 베이스로 들어왔어요')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('메시지'), { target: { value: '안녕하세요!' } });
    fireEvent.click(screen.getByRole('button', { name: '보내기' }));
    expect(screen.getByText('안녕하세요!')).toBeTruthy(); // 서버 응답 전에 이미 보인다
    await waitFor(() => expect(screen.queryByText('보내는 중')).toBeNull());

    act(() => emit('message', msg(12, { body: '어서 와요 🎸' })));
    expect(await screen.findByText('어서 와요 🎸')).toBeTruthy();
    act(() => emit('message', msg(12, { body: '어서 와요 🎸' }))); // 같은 메시지가 또 와도 하나
    expect(screen.getAllByText('어서 와요 🎸')).toHaveLength(1);

    const reads = calls.filter((c) => c.url.endsWith('/read')).map((c) => JSON.parse(c.init!.body as string).lastId);
    await waitFor(() => expect(Math.max(...reads, 0)).toBe(12));
    const sent = calls.find((c) => c.init?.method === 'POST' && c.url === '/api/bands/3/messages')!;
    expect(JSON.parse(sent.init!.body as string).clientId).toMatch(/^[A-Za-z0-9-]{8,36}$/);
  });

  it('보내기에 실패하면 다시 보낼 수 있다', async () => {
    let fail = true;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      if (url === '/api/bands/3') return json({ title: '밴드', members: [] });
      if (url === '/api/bands/3/messages' && init?.method === 'POST') {
        if (fail) return json({ message: '잠시 후 다시' }, 500);
        const b = JSON.parse(init.body as string);
        return json(msg(20, { senderId: 7, body: b.body, clientId: b.clientId }));
      }
      if (url === '/api/bands/3/messages') return json({ messages: [], hasMore: false, lastReadId: null });
      return json(undefined, 204);
    }));
    render(
      <StreamContext.Provider value={fakeStream().stream}>
        <MemoryRouter initialEntries={['/bands/3/chat']}>
          <Routes><Route path="/bands/:id/chat" element={<ChatPage me={me} />} /></Routes>
        </MemoryRouter>
      </StreamContext.Provider>,
    );
    await screen.findByText('첫 메시지를 남겨 보세요.');
    fireEvent.change(screen.getByLabelText('메시지'), { target: { value: '합주 몇 시?' } });
    fireEvent.click(screen.getByRole('button', { name: '보내기' }));
    expect(await screen.findByText('보내지 못했어요')).toBeTruthy();
    fail = false;
    fireEvent.click(screen.getByRole('button', { name: '다시 보내기' }));
    await waitFor(() => expect(screen.queryByText('보내지 못했어요')).toBeNull());
    expect(screen.getAllByText('합주 몇 시?')).toHaveLength(1);
  });
});
