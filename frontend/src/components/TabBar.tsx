import { Bell, Compass, Music2, UserRound } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useStream } from '../lib/stream';

const TABS = [
  { to: '/', label: '내 밴드', end: true, Icon: Music2 },
  { to: '/explore', label: '찾기', end: false, Icon: Compass },
  { to: '/notifications', label: '알림', end: false, Icon: Bell },
  { to: '/me', label: '마이', end: false, Icon: UserRound },
];

export default function TabBar() {
  const { unread } = useStream();
  return (
    <nav className="tabbar" aria-label="주요 메뉴">
      {TABS.map((t) => (
        <NavLink key={t.to} to={t.to} end={t.end} className={({ isActive }) => (isActive ? 'on' : '')}>
          <t.Icon aria-hidden="true" />
          {t.label}
          {t.to === '/notifications' && unread > 0 && (
            <span className="tab-badge" aria-label={`안 읽은 알림 ${unread}개`}>{unread > 99 ? '99+' : unread}</span>
          )}
        </NavLink>
      ))}
    </nav>
  );
}
