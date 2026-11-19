import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';
import type { Me } from '../api/types';
import { LEVELS, instrumentName } from '../lib/profile';

/** 1단계 홈: 로그인·프로필이 서버와 잘 이어졌는지 확인하는 자리. 다음 단계에서 '내 밴드' 홈으로 바뀐다. */
export default function HomePage({ me, onLogout }: { me: Me; onLogout: () => void }) {
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
      <header className="top pad">
        <div className="between">
          <span className="logo">밴드<em>몽</em></span>
          <button className="btn-ghost" onClick={out}>로그아웃</button>
        </div>
      </header>
      <div className="body pad">
        <section className="card">
          <div className="between">
            <h1 className="h1">{me.nickname}</h1>
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
          <p className="sub">밴드 만들기·찾기, 합주 일정, 채팅이 차례로 서버에 붙어요.</p>
        </section>
      </div>
    </main>
  );
}
