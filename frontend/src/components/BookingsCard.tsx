import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cancelBooking, listBookings, setSettle } from '../api/studios';
import type { BookingView } from '../api/types';
import { useLoad } from '../lib/useLoad';
import { won } from '../lib/money';
import { fmtDateTime, fmtTime } from '../lib/time';

/** 밴드 상세: 합주실 예약과 1인 정산 */
export default function BookingsCard({ bandId, isHost }: { bandId: number; isHost: boolean }) {
  const { data, setData, error, reload } = useLoad(() => listBookings(bandId), [bandId]);
  const [msg, setMsg] = useState<string | null>(null);

  async function run(fn: () => Promise<BookingView>) {
    setMsg(null);
    try {
      const next = await fn();
      setData((cur) => cur && cur.map((b) => (b.id === next.id ? next : b)));
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '처리하지 못했어요');
      reload();
    }
  }

  const list = (data ?? []).filter((b) => b.status === 'CONFIRMED');
  return (
    <section className="card">
      <div className="between">
        <h2 className="section">합주실</h2>
        {isHost && <Link className="btn-dark" to={`/bands/${bandId}/studio`}>예약하기</Link>}
      </div>
      {error && <p className="err-text">{error}</p>}
      {data && list.length === 0 && <p className="sub">예약된 합주실이 없어요.{isHost ? '' : ' 예약은 방장이 해요.'}</p>}
      {list.map((b) => (
        <article key={b.id} className="booking">
          <div className="between">
            <b>{b.studioName} {b.roomName}</b>
            {isHost && new Date(b.startsAt).getTime() > Date.now() && (
              <button className="link-btn" onClick={() => run(() => cancelBooking(b.id))}>취소</button>
            )}
          </div>
          <span className="sub">{fmtDateTime(b.startsAt)}–{fmtTime(b.endsAt)} · 총 {won(b.price)} · 1인 {won(b.perPerson)}</span>
          <div className="settle-row">
            <span className="sub">정산 {b.paidCount}/{b.settles.length}</span>
            <div className="row wrap">
              {b.settles.map((s) => {
                const can = isHost || s.userId === b.mine?.userId;
                return (
                  <button key={s.userId} className={`settle ${s.paid ? 'paid' : ''}`} disabled={!can}
                          aria-pressed={s.paid} aria-label={`${s.nickname} ${s.paid ? '냄' : '안 냄'}`}
                          onClick={() => run(() => setSettle(b.id, s.userId, !s.paid))}>
                    {s.paid ? '✓ ' : ''}{s.nickname}
                  </button>
                );
              })}
            </div>
          </div>
          {b.mine && !b.mine.paid && <p className="sub">내 몫 {won(b.mine.amount)} — 보냈으면 내 이름을 눌러 표시해 주세요.</p>}
        </article>
      ))}
      {msg && <p className="err-text" role="alert">{msg}</p>}
    </section>
  );
}
