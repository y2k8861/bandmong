import { NavLink } from 'react-router-dom';

const TABS = [
  { to: '/', label: '내 밴드', end: true },
  { to: '/explore', label: '찾기', end: false },
  { to: '/me', label: '마이', end: false },
];

export default function TabBar() {
  return (
    <nav className="tabbar" aria-label="주요 메뉴">
      {TABS.map((t) => (
        <NavLink key={t.to} to={t.to} end={t.end} className={({ isActive }) => (isActive ? 'on' : '')}>
          {t.label}
        </NavLink>
      ))}
    </nav>
  );
}
