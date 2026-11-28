import { useState } from 'react';
import { Bell, CalendarCheck, CalendarClock, CalendarX, DoorOpen, Mail, PartyPopper, PencilLine, UserPlus, type LucideIcon } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getNotifications, readAllNotifications, readNotification } from '../api/notifications';
import type { NotificationView } from '../api/types';
import { ago } from '../lib/band';
import { useStream, useStreamEvent } from '../lib/stream';
import { useLoad } from '../lib/useLoad';

const ICON: Record<string, LucideIcon> = {
  APPLICATION_RECEIVED: UserPlus, APPLICATION_ACCEPTED: PartyPopper, APPLICATION_REJECTED: Mail, MEMBER_LEFT: DoorOpen,
  JAM_SCHEDULED: CalendarCheck, JAM_MOVED: CalendarClock, JAM_EDITED: PencilLine, JAM_CANCELED: CalendarX,
};

export default function NotificationsPage() {
  const nav = useNavigate();
  const { setUnread, unread } = useStream();
  const { data, setData, error, reload } = useLoad(getNotifications, []);
  const [more, setMore] = useState(false);

  useStreamEvent('notification', (d) => setData((cur) => (cur ? { ...cur, items: [d.item, ...cur.items.filter((x) => x.id !== d.item.id)], unread: d.unread } : cur)));
  useStreamEvent('reconnect', () => { reload(); });

  async function open(n: NotificationView) {
    if (!n.read) {
      setData((cur) => cur && { ...cur, items: cur.items.map((x) => (x.id === n.id ? { ...x, read: true } : x)) });
      setUnread(Math.max(0, unread - 1));
      readNotification(n.id).catch(() => undefined);
    }
    if (n.link) nav(n.link);
  }

  async function readAll() {
    await readAllNotifications();
    setUnread(0);
    setData((cur) => cur && { ...cur, unread: 0, items: cur.items.map((x) => ({ ...x, read: true })) });
  }

  async function loadMore() {
    if (!data) return;
    setMore(true);
    try {
      const next = await getNotifications(data.items[data.items.length - 1].id);
      setData({ ...data, items: [...data.items, ...next.items], hasMore: next.hasMore });
    } finally {
      setMore(false);
    }
  }

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <h1 className="h1">알림</h1>
          {unread > 0 && <button className="btn-ghost" onClick={readAll}>모두 읽음</button>}
        </div>
      </header>
      <div className="body pad">
        {error && <div className="card"><p className="err-text">{error}</p><button className="btn-ghost" onClick={reload}>다시 시도</button></div>}
        {data && data.items.length === 0 && (
          <section className="card empty"><b>아직 알림이 없어요</b><p className="sub">지원 결과, 합주 확정 같은 소식이 여기로 와요.</p></section>
        )}
        {data && data.items.length > 0 && (
          <ul className="noti-list">
            {data.items.map((n) => {
              const Icon = ICON[n.type] ?? Bell;
              return (
              <li key={n.id}>
                <button className={`noti ${n.read ? '' : 'unread'}`} onClick={() => open(n)}>
                  <span className="ic" aria-hidden><Icon /></span>
                  <span className="txt">
                    <b>{n.title}</b>
                    {n.body && <span className="sub">{n.body}</span>}
                    <span className="when">{ago(n.createdAt)}</span>
                  </span>
                  {!n.read && <span className="visually-hidden">안 읽음</span>}
                </button>
              </li>
              );
            })}
          </ul>
        )}
        {data?.hasMore && <button className="btn-ghost wide" disabled={more} onClick={loadMore}>{more ? '불러오는 중…' : '더 보기'}</button>}
      </div>
    </main>
  );
}
