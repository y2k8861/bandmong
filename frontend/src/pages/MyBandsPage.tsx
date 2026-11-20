import { Link } from 'react-router-dom';
import { myBands } from '../api/bands';
import type { Me } from '../api/types';
import BandCard from '../components/BandCard';
import { useLoad } from '../lib/useLoad';
import { useStreamEvent } from '../lib/stream';

export default function MyBandsPage({ me }: { me: Me }) {
  const { data, error, loading, reload } = useLoad(myBands, []);
  // 새 메시지·알림(수락, 합주 확정 등)이 오면 카드 숫자를 새로 읽는다
  useStreamEvent('message', () => { reload(); });
  useStreamEvent('notification', () => { reload(); });
  useStreamEvent('reconnect', () => { reload(); });

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <span className="logo">밴드<em>몽</em></span>
          <Link className="btn-dark" to="/bands/new">+ 밴드 만들기</Link>
        </div>
        <p className="sub">{me.nickname}님, 오늘도 퇴근하고 합주 어때요?</p>
      </header>
      <div className="body pad">
        {loading && !data && <p className="sub">불러오는 중…</p>}
        {error && <div className="card"><p className="err-text">{error}</p><button className="btn-ghost" onClick={reload}>다시 시도</button></div>}
        {data && data.bands.length === 0 && data.applied.length === 0 && (
          <section className="card empty">
            <b>아직 속한 밴드가 없어요</b>
            <p className="sub">근처에서 내 악기 자리가 빈 밴드를 찾거나, 직접 밴드를 열어 보세요.</p>
            <div className="row">
              <Link className="cta small" to="/explore">밴드 찾기</Link>
              <Link className="btn-ghost" to="/bands/new">밴드 만들기</Link>
            </div>
          </section>
        )}
        {data && data.bands.length > 0 && (
          <>
            <h2 className="section">내 밴드 <span className="mono">{data.bands.length}</span></h2>
            {data.bands.map((b) => <BandCard key={b.id} band={b} />)}
          </>
        )}
        {data && data.applied.length > 0 && (
          <>
            <h2 className="section">지원한 밴드 <span className="mono">{data.applied.length}</span></h2>
            {data.applied.map((b) => <BandCard key={b.id} band={b} />)}
          </>
        )}
      </div>
    </main>
  );
}
