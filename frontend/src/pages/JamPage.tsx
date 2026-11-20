import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { answerJam, cancelJam, getJam, updateJam } from '../api/jams';
import type { JamView, RsvpAnswer } from '../api/types';
import JamFormSheet from '../components/JamFormSheet';
import Sheet from '../components/Sheet';
import { instrumentName } from '../lib/profile';
import { ANSWERS, answerName, dday, fmtDateTime, fmtTime, toKstInput } from '../lib/time';
import { useLoad } from '../lib/useLoad';

export default function JamPage() {
  const id = Number(useParams().id);
  const nav = useNavigate();
  const { data: jam, setData, error, loading, reload } = useLoad(() => getJam(id), [id]);
  const [sheet, setSheet] = useState<null | 'edit' | 'cancel'>(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  async function act(fn: () => Promise<JamView>, done?: string) {
    setBusy(true);
    try {
      setData(await fn());
      setSheet(null);
      if (done) setToast(done);
    } catch (e) {
      setToast(e instanceof Error ? e.message : '처리하지 못했어요');
      reload();
    } finally {
      setBusy(false);
      setTimeout(() => setToast(null), 2600);
    }
  }

  if (loading && !jam) return <main className="scr center"><p className="sub">불러오는 중…</p></main>;
  if (error && !jam) return <main className="scr center pad"><p className="err-text">{error}</p><button className="btn-ghost" onClick={() => nav(-1)}>돌아가기</button></main>;
  if (!jam) return null;

  const canceled = jam.status === 'CANCELED';
  const locked = canceled || jam.over || new Date(jam.startsAt).getTime() <= Date.now();
  const groups: { key: RsvpAnswer | null; label: string }[] = [
    { key: 'YES', label: '참석' }, { key: 'LATE', label: '늦어요' }, { key: 'NO', label: '불참' }, { key: null, label: '아직 답 안 함' },
  ];

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <Link className="btn-ghost" to={`/bands/${jam.bandId}`}>← {jam.bandTitle}</Link>
          {jam.host && !locked && <button className="btn-ghost" onClick={() => setSheet('edit')}>바꾸기</button>}
        </div>
        <div className="row">
          {canceled ? <span className="badge">취소됨</span> : jam.over ? <span className="tag soft">지난 합주</span> : <span className="dday">{dday(jam.startsAt)}</span>}
        </div>
        <h1 className="h1" style={canceled ? { textDecoration: 'line-through', color: 'var(--mute)' } : undefined}>{fmtDateTime(jam.startsAt)}–{fmtTime(jam.endsAt)}</h1>
        <p className="sub">{jam.place ?? '장소 미정'}</p>
      </header>

      <div className="body pad">
        {jam.memo && <section className="card"><p className="intro">{jam.memo}</p></section>}

        {!locked && (
          <section className="card">
            <h2 className="section">나는 <span>{answerName(jam.myAnswer)}</span></h2>
            <div className="answer-row" role="group" aria-label="참석 여부">
              {ANSWERS.map((a) => (
                <button key={a.id} className={`ans ${a.id.toLowerCase()}`} aria-pressed={jam.myAnswer === a.id} disabled={busy}
                        onClick={() => act(() => answerJam(jam.id, a.id), `${a.name}으로 답했어요`)}>
                  {a.name}
                </button>
              ))}
            </div>
          </section>
        )}

        <section className="card">
          <h2 className="section">멤버 응답
            <span className="mono">참석 {jam.counts.yes} · 늦음 {jam.counts.late} · 불참 {jam.counts.no}</span>
          </h2>
          {groups.map((g) => {
            const people = (jam.answers ?? []).filter((a) => a.answer === g.key);
            if (!people.length) return null;
            return (
              <div key={g.label} className="ans-group">
                <span className={`ans-label ${(g.key ?? 'none').toLowerCase()}`}>{g.label} {people.length}</span>
                <div className="row wrap">
                  {people.map((p) => <span key={p.userId} className="tag soft">{p.nickname} · {instrumentName(p.instrument)}</span>)}
                </div>
              </div>
            );
          })}
        </section>

        {jam.host && !locked && (() => {
          const k = toKstInput(jam.startsAt);
          const q = `date=${k.date}&hour=${Number(k.time.slice(0, 2))}&hours=${Math.max(1, Math.round(jam.durationMinutes / 60))}&jam=${jam.id}`;
          return <Link className="btn-dark wide" to={`/bands/${jam.bandId}/studio?${q}`}>이 시간으로 합주실 잡기</Link>;
        })()}
        {jam.host && !locked && (
          <button className="btn-ghost wide danger-text" onClick={() => setSheet('cancel')}>합주 취소</button>
        )}
      </div>

      {sheet === 'edit' && (
        <JamFormSheet title="합주 바꾸기"
                      initial={{ startsAt: jam.startsAt, durationMinutes: jam.durationMinutes, place: jam.place ?? '', memo: jam.memo ?? '' }}
                      submitLabel="저장" onClose={() => setSheet(null)}
                      onSubmit={async (v) => {
                        const moved = v.startsAt !== undefined && new Date(v.startsAt).getTime() !== new Date(jam.startsAt).getTime();
                        setData(await updateJam(jam.id, moved ? v : { durationMinutes: v.durationMinutes, place: v.place, memo: v.memo }));
                        setSheet(null);
                        setToast(moved ? '시간이 바뀌어서 참석 응답을 다시 받아요' : '저장했어요');
                        setTimeout(() => setToast(null), 2600);
                      }} />
      )}
      {sheet === 'cancel' && (
        <Sheet title="합주를 취소할까요?" onClose={() => setSheet(null)}>
          <p className="sub">멤버들에게 취소된 합주로 보여요. 되돌릴 수 없어요.</p>
          <button className="cta danger" disabled={busy} onClick={() => act(() => cancelJam(jam.id), '합주를 취소했어요')}>취소하기</button>
        </Sheet>
      )}
      {toast && <div className="toast" role="status">{toast}</div>}
    </main>
  );
}
