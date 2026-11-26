import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { withdraw } from '../api/moderation';
import { listBlocks, unblockUser } from '../api/app';
import { getProviders } from '../api/auth';
import { setToken } from '../lib/platform';
import { useLoad } from '../lib/useLoad';
import Sheet from '../components/Sheet';
import { fmtDateTime } from '../lib/time';
import { logout } from '../api/auth';
import type { Me } from '../api/types';
import { LEVELS, instrumentName } from '../lib/profile';

export default function MePage({ me, onLogout }: { me: Me; onLogout: () => void }) {
  const nav = useNavigate();

  const [leaving, setLeaving] = useState(false);
  const blocks = useLoad(listBlocks, []);
  const info = useLoad(getProviders, []);
  const [leaveErr, setLeaveErr] = useState<string | null>(null);

  async function doWithdraw() {
    setLeaveErr(null);
    try {
      await withdraw();
      await setToken(null);
      onLogout();
      nav('/login', { replace: true });
    } catch (e) {
      setLeaveErr(e instanceof Error ? e.message : '탈퇴하지 못했어요');
    }
  }

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
        {me.restrictedUntil && (
          <section className="card restricted" role="status">
            <b>{fmtDateTime(me.restrictedUntil)}까지 이용이 제한돼요</b>
            <p className="sub">신고가 접수되어 운영 정책에 따라 제한됐어요. 그동안은 보기만 할 수 있어요. 알림 탭에서 이유를 확인하세요.</p>
          </section>
        )}
        {me.role === 'ADMIN' && <Link className="card admin-link" to="/admin"><b>신고 처리</b><span className="sub">관리자 →</span></Link>}
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
        <section className="card">
          <h2 className="section">차단한 사람 <span className="mono">{blocks.data?.length ?? 0}</span></h2>
          {blocks.data && blocks.data.length === 0 && <p className="sub">차단한 사람이 없어요. 밴드 멤버의 ⋯ 메뉴나 채팅 메시지에서 차단할 수 있어요.</p>}
          {blocks.data?.map((b) => (
            <div key={b.userId} className="between">
              <span>{b.nickname}</span>
              <button className="btn-ghost" onClick={async () => blocks.setData(await unblockUser(b.userId))}>차단 풀기</button>
            </div>
          ))}
        </section>
        {info.data?.supportEmail && (
          <section className="card">
            <h2 className="section">문의</h2>
            <p className="sub">불편한 점이나 신고할 일이 있으면 알려 주세요.</p>
            <b className="mono select">{info.data.supportEmail}</b>
          </section>
        )}
        <button className="btn-ghost wide" onClick={out}>로그아웃</button>
        <button className="link-btn center-text" onClick={() => setLeaving(true)}>탈퇴하기</button>
        {leaving && (
          <Sheet title="밴드몽을 탈퇴할까요?" onClose={() => { setLeaving(false); setLeaveErr(null); }}>
            <p className="sub">
              프로필(닉네임·악기·지역)이 지워지고 모든 밴드에서 나가요. 같은 계정으로 다시 오면 새 회원으로 시작해요.
              채팅·합주 기록은 "탈퇴한 사용자"로 남아요. 방장인 밴드에 다른 멤버가 있으면 먼저 방장을 넘겨 주세요.
            </p>
            {leaveErr && <p className="err-text" role="alert">{leaveErr}</p>}
            <button className="cta danger" onClick={doWithdraw}>탈퇴하기</button>
          </Sheet>
        )}
      </div>
    </main>
  );
}
