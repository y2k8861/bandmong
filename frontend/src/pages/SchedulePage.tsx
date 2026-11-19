import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getBand } from '../api/bands';
import { createJam, getAvailability, saveAvailability } from '../api/jams';
import JamFormSheet from '../components/JamFormSheet';
import { useLoad } from '../lib/useLoad';
import { DAYS, addDays, fmtDateTime, fmtShortDate, slotIso, slotTime, thisMonday } from '../lib/time';

const WEEKS = ['이번 주', '다음 주', '다다음 주'];

/** 가능 시간 투표: 칸을 눌러 내 가능 시간을 고르고, 겹치는 인원을 색으로 본다. 방장은 가장 좋은 시간으로 확정. */
export default function SchedulePage() {
  const bandId = Number(useParams().id);
  const nav = useNavigate();
  const [weekIdx, setWeekIdx] = useState(0);
  const week = addDays(thisMonday(), weekIdx * 7);
  const band = useLoad(() => getBand(bandId), [bandId]);
  const { data, setData, error, loading, reload } = useLoad(() => getAvailability(bandId, week), [bandId, week]);
  const [mine, setMine] = useState<Set<number>>(new Set());
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [confirmAt, setConfirmAt] = useState<string | null>(null);

  useEffect(() => { if (data) setMine(new Set(data.mine)); }, [data]);

  const dirty = useMemo(() => {
    if (!data) return false;
    return data.mine.length !== mine.size || data.mine.some((s) => !mine.has(s));
  }, [data, mine]);

  const now = Date.now();
  const isHost = band.data?.me === 'HOST';

  function toggle(slot: number) {
    setMine((cur) => {
      const next = new Set(cur);
      if (next.has(slot)) next.delete(slot); else next.add(slot);
      return next;
    });
  }

  async function save() {
    setSaving(true);
    setMsg(null);
    try {
      setData(await saveAvailability(bandId, week, [...mine].sort((a, b) => a - b)));
      setMsg('내 가능 시간을 저장했어요');
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '저장하지 못했어요');
    } finally {
      setSaving(false);
    }
  }

  // 저장 전 내 선택까지 반영한 인원 (화면에서 바로 보이게)
  const countOf = (slot: number) => {
    if (!data) return 0;
    const base = data.counts[String(slot)] ?? 0;
    const was = data.mine.includes(slot) ? 1 : 0;
    return base - was + (mine.has(slot) ? 1 : 0);
  };
  const total = data?.memberCount ?? 1;

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <button className="btn-ghost" onClick={() => nav(`/bands/${bandId}`)}>← {band.data?.title ?? '밴드'}</button>
        </div>
        <h1 className="h1">가능 시간 투표</h1>
        <p className="sub">되는 칸을 눌러 고르고 저장하세요. 진할수록 많이 겹쳐요.</p>
        <div className="chips" role="group" aria-label="주 선택">
          {WEEKS.map((w, i) => (
            <button key={w} className="chip dark" aria-pressed={weekIdx === i} onClick={() => setWeekIdx(i)}>
              {w} · {fmtShortDate(addDays(thisMonday(), i * 7))}~
            </button>
          ))}
        </div>
      </header>

      <div className="body pad">
        {error && <div className="card"><p className="err-text">{error}</p><button className="btn-ghost" onClick={reload}>다시 시도</button></div>}
        {data && (
          <>
            <section className="card grid-card">
              <div className="grid" role="grid" aria-label="가능 시간" style={{ opacity: loading ? 0.5 : 1 }}>
                <div className="gh" />
                {DAYS.map((d, i) => (
                  <div key={d} className={`gh ${i >= 5 ? 'weekend' : ''}`}>{d}<span>{fmtShortDate(addDays(week, i)).split('/')[1]}</span></div>
                ))}
                {data.hours.map((h) => (
                  <Row key={h} h={h}>
                    {DAYS.map((_, day) => {
                      const slot = day * 24 + h;
                      const past = slotTime(week, slot) <= now;
                      const n = countOf(slot);
                      const pick = mine.has(slot);
                      return (
                        <button key={slot} role="gridcell" className={`cell ${pick ? 'pick' : ''}`} disabled={past}
                                aria-pressed={pick} aria-label={`${DAYS[day]} ${h}시, ${n}명 가능${pick ? ', 내가 고름' : ''}`}
                                style={{ ['--heat' as string]: String(n / total) }} onClick={() => toggle(slot)}>
                          {n > 0 && <span>{n}</span>}
                        </button>
                      );
                    })}
                  </Row>
                ))}
              </div>
              <div className="legend"><span className="cell pick" /> 내가 고른 칸 <span className="cell" style={{ ['--heat' as string]: '1' }} /> 모두 가능</div>
            </section>

            <section className="card">
              <h2 className="section">가장 많이 겹치는 시간</h2>
              {data.best.length === 0 && <p className="sub">아직 겹치는 시간이 없어요. 멤버들이 고르면 여기 나타나요.</p>}
              {data.best.map((b, i) => (
                <div key={b.slot} className="best">
                  <div>
                    <b>{i === 0 && '👑 '}{fmtDateTime(b.startsAt)}</b>
                    <p className="sub">{b.count}/{total}명 · {b.names.join(', ')}</p>
                  </div>
                  {isHost && <button className="cta small" onClick={() => setConfirmAt(slotIso(week, b.slot))}>이 시간으로 확정</button>}
                </div>
              ))}
              {!isHost && data.best.length > 0 && <p className="sub">확정은 방장이 해요.</p>}
            </section>

            <section className="card">
              <h2 className="section">투표 현황 <span className="mono">{data.voters.filter((v) => v.voted).length}/{data.voters.length}</span></h2>
              <div className="row wrap">
                {data.voters.map((v) => (
                  <span key={v.userId} className={`tag ${v.voted ? '' : 'soft'}`}>{v.nickname}{v.voted ? '' : ' · 아직'}</span>
                ))}
              </div>
            </section>
          </>
        )}
      </div>

      <div className="bar pad col">
        {msg && <p className="sub center-text" role="status">{msg}</p>}
        <button className="cta" disabled={!dirty || saving} onClick={save}>
          {saving ? '저장하는 중' : dirty ? `내 가능 시간 저장 (${mine.size}칸)` : '저장됨'}
        </button>
      </div>

      {confirmAt && (
        <JamFormSheet title="합주 확정" initial={{ startsAt: confirmAt, durationMinutes: 120, place: '', memo: '' }}
                      submitLabel="이 시간으로 확정" onClose={() => setConfirmAt(null)}
                      onSubmit={async (v) => { const jam = await createJam(bandId, v); nav(`/jams/${jam.id}`, { replace: true }); }} />
      )}
    </main>
  );
}

function Row({ h, children }: { h: number; children: ReactNode }) {
  return (
    <>
      <div className="rh">{h}시</div>
      {children}
    </>
  );
}
