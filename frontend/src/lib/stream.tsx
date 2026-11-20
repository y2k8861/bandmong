import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { getUnread } from '../api/notifications';
import type { MessageView, NotificationView } from '../api/types';

/**
 * 앱 전체가 공유하는 실시간 연결 하나 (/api/stream, SSE).
 * - 끊기면 브라우저(EventSource)가 알아서 다시 붙는다.
 * - 다시 붙었을 때는 그 사이 놓친 걸 화면들이 다시 읽도록 'reconnect'를 알린다.
 */
export type StreamEvents = {
  message: MessageView;
  notification: { item: NotificationView; unread: number };
  reconnect: null;
};
type Handler<K extends keyof StreamEvents> = (data: StreamEvents[K]) => void;

export interface Stream {
  subscribe<K extends keyof StreamEvents>(event: K, fn: Handler<K>): () => void;
  connected: boolean;
  unread: number;
  setUnread: (n: number) => void;
}

const noop: Stream = { subscribe: () => () => undefined, connected: false, unread: 0, setUnread: () => undefined };
export const StreamContext = createContext<Stream>(noop);
export const useStream = () => useContext(StreamContext);

/** 이벤트 구독. fn은 최신 것을 쓰므로 매 렌더 새로 만들어도 다시 구독하지 않는다. */
export function useStreamEvent<K extends keyof StreamEvents>(event: K, fn: Handler<K>) {
  const { subscribe } = useStream();
  const ref = useRef(fn);
  ref.current = fn;
  useEffect(() => subscribe(event, (d) => ref.current(d)), [subscribe, event]);
}

export function StreamProvider({ children }: { children: ReactNode }) {
  const listeners = useRef(new Map<string, Set<(d: unknown) => void>>());
  const [connected, setConnected] = useState(false);
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    getUnread().then((r) => setUnread(r.unread)).catch(() => undefined);
    if (typeof EventSource === 'undefined') return;

    const emit = (name: string, data: unknown) => listeners.current.get(name)?.forEach((fn) => fn(data));
    const es = new EventSource('/api/stream', { withCredentials: true });
    let dropped = false;

    es.addEventListener('ready', () => {
      setConnected(true);
      if (dropped) {
        dropped = false;
        emit('reconnect', null);
        getUnread().then((r) => setUnread(r.unread)).catch(() => undefined);
      }
    });
    es.addEventListener('message', (e) => emit('message', JSON.parse((e as MessageEvent).data)));
    es.addEventListener('notification', (e) => {
      const d = JSON.parse((e as MessageEvent).data);
      setUnread(d.unread);
      emit('notification', d);
    });
    es.onerror = () => {
      setConnected(false);
      dropped = true;
    };
    return () => es.close();
  }, []);

  const subscribe = useCallback<Stream['subscribe']>((event, fn) => {
    const set = listeners.current.get(event) ?? new Set();
    set.add(fn as (d: unknown) => void);
    listeners.current.set(event, set);
    return () => { set.delete(fn as (d: unknown) => void); };
  }, []);

  const value = useMemo<Stream>(() => ({ connected, unread, setUnread, subscribe }), [connected, unread, subscribe]);

  return <StreamContext.Provider value={value}>{children}</StreamContext.Provider>;
}
