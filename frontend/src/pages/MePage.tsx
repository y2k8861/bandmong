import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';
import type { Me } from '../api/types';
import { LEVELS, instrumentName } from '../lib/profile';

export default function MePage({ me, onLogout }: { me: Me; onLogout: () => void }) {
  const nav = useNavigate();

  async function out() {
    try {
      await logout();
    } finally {
      onLogout();
      nav('/login', { replace: true });
    }
  }

  return (
    <main className="scr">
      <header className="top pad"><h1 className="h1">마이</h1></header>
      <div className="body pad">
        <section className="card">
          <div className="between">
            <h2 className="h1">{me.nickname}</h2>
            <button className="btn-dark" onClick={() => nav('/onboarding')}>프로필 수정</button>
          </div>
          <div className="row wrap">
            {me.instruments.map((i) => <span key={i} className="tag">{instrumentName(i)}</span>)}
            <span className="tag soft">{LEVELS.find((l) => l.id === me.level)?.name}</span>
            {me.region && <span className="tag soft">{me.region}</span>}
          </div>
        </section>
        <section className="card muted">
          <b>다음 단계에서 열려요</b>
          <p className="sub">합주 기록, 칭찬 스티커와 합주 온도가 여기에 붙어요.</p>
        </section>
        <button className="btn-ghost wide" onClick={out}>로그아웃</button>
      </div>
    </main>
  );
}
