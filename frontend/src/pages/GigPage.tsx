import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { addCheck, cancelGig, deleteCheck, getGig, markCheck, saveCue } from '../api/gigs';
import type { CueItem, CueType, GigView } from '../api/types';
import Sheet from '../components/Sheet';
import { cueTimes, mmss, move, totalSec } from '../lib/cue';
import { dday, fmtDateTime } from '../lib/time';
import { useLoad } from '../lib/useLoad';

export default function GigPage() {
  const id = Number(useParams().id);
  const { data: gig, setData, error, reload } = useLoad(() => getGig(id), [id]);
  const [draft, setDraft] = useState<CueItem[] | null>(null); // null이면 고치는 중 아님
  const [stale, setStale] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [adding, setAdding] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [checkText, setCheckText] = useState('');

  useEffect(() => { setDraft(null); setStale(false); }, [id]);

  async function run(fn: () => Promise<GigView>) {
    setMsg(null);
    try { setData(await fn()); } catch (e) { setMsg(e instanceof Error ? e.message : '처리하지 못했어요'); }
  }

  async function save() {
    if (!gig || !draft) return;
    setBusy(true);
    setMsg(null);
    try {
      setData(await saveCue(gig.id, gig.version, draft));
      setDraft(null);
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) setStale(true);
      setMsg(e instanceof Error ? e.message : '저장하지 못했어요');
    } finally {
      setBusy(false);
    }
  }

  if (error && !gig) return <main className="scr center pad"><p className="err-text">{error}</p></main>;
  if (!gig) return <main className="scr center"><p className="sub">불러오는 중…</p></main>;

  const canceled = gig.status === 'CANCELED';
  const items = draft ?? gig.items;
  const times = cueTimes(gig.startsAt, items);
  const tot = totalSec(items), slot = gig.slotMinutes * 60, over = tot > slot;
  const edit = (fn: (cur: CueItem[]) => CueItem[]) => setDraft((cur) => fn(cur ?? gig.items));

  return (
    <main className="scr">
      <header className="top pad dark-head">
        <div className="between">
          <Link className="btn-ghost on-dark" to={`/bands/${gig.bandId}`}>← {gig.bandTitle}</Link>
          {canceled ? <span className="badge">취소됨</span> : <span className="dday">{dday(gig.startsAt)}</span>}
        </div>
        <h1 className="h1">{gig.title}</h1>
        <p className="sub">{fmtDateTime(gig.startsAt)} · {gig.venue ?? '공연장 미정'} · 우리 순서 {gig.slotMinutes}분</p>
        <div className={`timebar ${over ? 'over' : ''}`} role="img" aria-label={`큐시트 ${mmss(tot)} / ${gig.slotMinutes}분`}>
          <span style={{ width: `${Math.min(100, (tot / slot) * 100)}%` }} />
        </div>
        <p className="sub mono">큐시트 길이 {mmss(tot)} / {gig.slotMinutes}:00 {over ? `· ${mmss(tot - slot)} 넘어요` : `· ${mmss(slot - tot)} 남아요`}</p>
      </header>

      <div className="body pad">
        <section className="card">
          <div className="between">
            <h2 className="section">큐시트 <span className="mono">곡 {items.filter((x) => x.type === 'SONG').length}</span></h2>
            {!canceled && !draft && <button className="btn-ghost" onClick={() => setDraft(gig.items)}>고치기</button>}
          </div>
          <ol className="cue">
            {items.map((it, i) => (
              <li key={`${i}-${it.title}`} className={`cue-row ${it.type === 'MC' ? 'mc' : ''}`}>
                <span className="mono at">{times[i]}</span>
                <span className="cue-main">
                  <b>{it.type === 'MC' && <span className="tag soft">멘트</span>} {it.title}</b>
                  <span className="mono sub">{mmss(it.durationSec)}</span>
                </span>
                {draft && (
                  <span className="cue-ctl">
                    <button aria-label={`${it.title} 15초 줄이기`} disabled={it.durationSec <= 15} onClick={() => edit((c) => c.map((x, j) => (j === i ? { ...x, durationSec: x.durationSec - 15 } : x)))}>−</button>
                    <button aria-label={`${it.title} 15초 늘리기`} disabled={it.durationSec >= 1200} onClick={() => edit((c) => c.map((x, j) => (j === i ? { ...x, durationSec: x.durationSec + 15 } : x)))}>+</button>
                    <button aria-label={`${it.title} 위로`} disabled={i === 0} onClick={() => edit((c) => move(c, i, -1))}>↑</button>
                    <button aria-label={`${it.title} 아래로`} disabled={i === items.length - 1} onClick={() => edit((c) => move(c, i, 1))}>↓</button>
                    <button aria-label={`${it.title} 빼기`} className="del" onClick={() => edit((c) => c.filter((_, j) => j !== i))}>✕</button>
                  </span>
                )}
              </li>
            ))}
          </ol>
          {draft && <button className="btn-ghost wide" onClick={() => setAdding(true)}>+ 곡·멘트 넣기</button>}
        </section>

        <section className="card">
          <h2 className="section">준비 <span className="mono">{gig.doneCount}/{gig.checks.length}</span></h2>
          <ul className="checks">
            {gig.checks.map((c) => (
              <li key={c.id}>
                <label className={c.done ? 'done' : ''}>
                  <input type="checkbox" checked={c.done} disabled={canceled} onChange={() => run(() => markCheck(gig.id, c.id, !c.done))} />
                  <span>{c.text}{c.doneBy && <span className="sub"> · {c.doneBy}</span>}</span>
                </label>
                {!canceled && <button className="x" aria-label={`${c.text} 지우기`} onClick={() => run(() => deleteCheck(gig.id, c.id))}>✕</button>}
              </li>
            ))}
          </ul>
          {!canceled && (
            <form className="search" onSubmit={(e: FormEvent) => { e.preventDefault(); if (checkText.trim()) { run(() => addCheck(gig.id, checkText)); setCheckText(''); } }}>
              <input className="input" maxLength={60} placeholder="할 일 추가 (예: 인이어 배터리)" aria-label="할 일 추가" value={checkText} onChange={(e) => setCheckText(e.target.value)} />
              <button className="btn-dark">추가</button>
            </form>
          )}
        </section>

        {gig.host && !canceled && <button className="btn-ghost wide danger-text" onClick={() => setConfirmCancel(true)}>공연 취소</button>}
        {!draft && msg && <p className="err-text" role="alert">{msg}</p>}
      </div>

      {draft && (
        <div className="bar pad col">
          {msg && <p className="err-text center-text" role="alert">{msg}</p>}
          <div className="row">
            {stale ? (
              <button className="cta" onClick={() => { setDraft(null); setStale(false); setMsg(null); reload(); }}>최신으로 불러오기 (내가 고친 건 사라져요)</button>
            ) : (
              <>
                <button className="btn-ghost" onClick={() => { setDraft(null); setMsg(null); }}>그만두기</button>
                <button className="cta" disabled={busy} onClick={save}>{busy ? '저장하는 중' : '큐시트 저장'}</button>
              </>
            )}
          </div>
        </div>
      )}

      {adding && <AddCueSheet onClose={() => setAdding(false)} onAdd={(it) => { edit((c) => [...c.slice(0, Math.max(0, c.length - 1)), it, ...c.slice(Math.max(0, c.length - 1))]); setAdding(false); }} />}
      {confirmCancel && (
        <Sheet title="공연을 취소할까요?" onClose={() => setConfirmCancel(false)}>
          <p className="sub">멤버들에게 취소된 공연으로 보여요. 되돌릴 수 없어요.</p>
          <button className="cta danger" onClick={() => { run(() => cancelGig(gig.id)); setConfirmCancel(false); }}>취소하기</button>
        </Sheet>
      )}
    </main>
  );
}

/** 새 줄은 마무리 인사 앞에 넣는다 */
function AddCueSheet({ onClose, onAdd }: { onClose: () => void; onAdd: (it: CueItem) => void }) {
  const [type, setType] = useState<CueType>('SONG');
  const [title, setTitle] = useState('');
  const [min, setMin] = useState('4');
  const [sec, setSec] = useState('00');
  const dur = Number(min || 0) * 60 + Number(sec || 0);
  const ok = title.trim() && dur >= 15 && dur <= 1200;
  return (
    <Sheet title="곡·멘트 넣기" onClose={onClose}>
      <div className="row" role="group" aria-label="종류">
        <button className="chip" aria-pressed={type === 'SONG'} onClick={() => { setType('SONG'); setMin('4'); setSec('00'); }}>곡</button>
        <button className="chip" aria-pressed={type === 'MC'} onClick={() => { setType('MC'); setMin('0'); setSec('30'); }}>멘트</button>
      </div>
      <div className="field">
        <label className="label" htmlFor="cue-t">{type === 'SONG' ? '곡 이름' : '멘트 내용'}</label>
        <input id="cue-t" className="input" maxLength={60} value={title} placeholder={type === 'SONG' ? '예: 우리 밴드 첫 곡' : '예: 다음 곡 소개'} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="field">
        <span className="label">길이</span>
        <div className="row">
          <input className="input mono" style={{ width: 70 }} inputMode="numeric" aria-label="분" value={min} onChange={(e) => setMin(e.target.value.replace(/\D/g, '').slice(0, 2))} />분
          <input className="input mono" style={{ width: 70 }} inputMode="numeric" aria-label="초" value={sec} onChange={(e) => setSec(e.target.value.replace(/\D/g, '').slice(0, 2))} />초
        </div>
      </div>
      <button className="cta" disabled={!ok} onClick={() => onAdd({ type, title: title.trim(), durationSec: dur })}>넣기</button>
    </Sheet>
  );
}
