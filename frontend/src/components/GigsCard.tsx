import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createGig, listGigs } from '../api/gigs';
import { useLoad } from '../lib/useLoad';
import { mmss } from '../lib/cue';
import { addDays, dday, fmtDateTime, fromKstInput, todayKst } from '../lib/time';
import Sheet from './Sheet';

export default function GigsCard({ bandId, isHost }: { bandId: number; isHost: boolean }) {
  const { data, error } = useLoad(() => listGigs(bandId), [bandId]);
  const [open, setOpen] = useState(false);
  const nav = useNavigate();
  return (
    <section className="card">
      <div className="between">
        <h2 className="section">공연</h2>
        {isHost && <button className="btn-dark" onClick={() => setOpen(true)}>공연 잡기</button>}
      </div>
      {error && <p className="err-text">{error}</p>}
      {data && data.length === 0 && <p className="sub">잡힌 공연이 없어요. 공연을 잡으면 큐시트와 준비 목록을 같이 만들어요.</p>}
      {data?.map((g) => (
        <Link key={g.id} to={`/gigs/${g.id}`} className={`jam-row ${g.status === 'CANCELED' ? 'canceled' : ''}`}>
          <span className="dday">{g.status === 'CANCELED' ? '취소' : dday(g.startsAt)}</span>
          <span className="jam-main">
            <b>{g.title}</b>
            <span className="sub">{fmtDateTime(g.startsAt)} · {g.venue ?? '공연장 미정'}</span>
            <span className={`sub ${g.totalSec > g.slotMinutes * 60 ? 'err-text' : ''}`}>큐시트 {mmss(g.totalSec)}/{g.slotMinutes}:00 · 준비 {g.doneCount}/{g.checks.length}</span>
          </span>
        </Link>
      ))}
      {open && (
        <NewGigSheet onClose={() => setOpen(false)}
                     onSubmit={async (v) => { const g = await createGig(bandId, v); nav(`/gigs/${g.id}`); }} />
      )}
    </section>
  );
}

function NewGigSheet({ onClose, onSubmit }: { onClose: () => void; onSubmit: (v: { title: string; startsAt: string; venue: string; slotMinutes: number }) => Promise<void> }) {
  const today = todayKst();
  const [title, setTitle] = useState('');
  const [date, setDate] = useState(addDays(today, 30));
  const [time, setTime] = useState('19:30');
  const [venue, setVenue] = useState('');
  const [slot, setSlot] = useState(30);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit() {
    setBusy(true);
    setError(null);
    try { await onSubmit({ title: title.trim(), startsAt: fromKstInput(date, time), venue: venue.trim(), slotMinutes: slot }); }
    catch (e) { setError(e instanceof Error ? e.message : '만들지 못했어요'); setBusy(false); }
  }
  return (
    <Sheet title="공연 잡기" onClose={onClose}>
      <div className="field">
        <label className="label" htmlFor="gt">공연 이름</label>
        <input id="gt" className="input" maxLength={30} placeholder="예: 연말 클럽 공연" value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="two">
        <div className="field"><label className="label" htmlFor="gd">날짜</label><input id="gd" className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} /></div>
        <div className="field"><label className="label" htmlFor="gtm">시작</label><input id="gtm" className="input" type="time" value={time} onChange={(e) => setTime(e.target.value)} /></div>
      </div>
      <div className="field">
        <label className="label" htmlFor="gv">공연장 <span>선택</span></label>
        <input id="gv" className="input" maxLength={40} value={venue} placeholder="예: 홍대 클럽 FF" onChange={(e) => setVenue(e.target.value)} />
      </div>
      <div className="field">
        <span className="label">우리 순서 길이</span>
        <div className="row wrap" role="group" aria-label="공연 시간">
          {[15, 20, 30, 40, 60].map((m) => <button key={m} type="button" className="chip" aria-pressed={slot === m} onClick={() => setSlot(m)}>{m}분</button>)}
        </div>
      </div>
      {error && <p className="err-text" role="alert">{error}</p>}
      <button className="cta" disabled={busy || !title.trim() || !date || !time} onClick={submit}>{busy ? '만드는 중' : '공연 만들기'}</button>
    </Sheet>
  );
}
