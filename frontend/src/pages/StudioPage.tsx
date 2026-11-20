import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getBand } from '../api/bands';
import { book, getSlots, getStudios } from '../api/studios';
import type { SlotsView } from '../api/types';
import { useLoad } from '../lib/useLoad';
import { hourLabel, perPerson, won } from '../lib/money';
import { DAYS, addDays } from '../lib/time';

const todayKst = () => new Date(Date.now() + 9 * 3600_000).toISOString().slice(0, 10);

/** 합주실 찾기 → 방 → 날짜 → 시간 고르기 → 예약. ?date=&hour=&hours=&jam= 으로 미리 채울 수 있다(합주 상세에서). */
export default function StudioPage() {
  const bandId = Number(useParams().id);
  const nav = useNavigate();
  const [q] = useSearchParams();
  const band = useLoad(() => getBand(bandId), [bandId]);
  const studios = useLoad(getStudios, []);
  const [area, setArea] = useState<string | null>(null);
  const [roomId, setRoomId] = useState<number | null>(null);
  const [date, setDate] = useState(q.get('date') ?? addDays(todayKst(), 1));
  const [slots, setSlots] = useState<SlotsView | null>(null);
  const [start, setStart] = useState<number | null>(q.get('hour') ? Number(q.get('hour')) : null);
  const [hours, setHours] = useState(Number(q.get('hours') ?? 2));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const jamId = q.get('jam') ? Number(q.get('jam')) : null;

  const areas = useMemo(() => [...new Set((studios.data ?? []).map((s) => s.area))], [studios.data]);
  const shown = (studios.data ?? []).filter((s) => !area || s.area === area);
  const room = (studios.data ?? []).flatMap((s) => s.rooms.map((r) => ({ ...r, studio: s }))).find((r) => r.id === roomId);

  useEffect(() => {
    if (!roomId) return;
    setSlots(null);
    getSlots(roomId, date).then(setSlots).catch((e) => setError(e.message));
  }, [roomId, date]);

  const picked = slots && start !== null ? slots.slots.filter((s) => s.hour >= start && s.hour < start + hours) : [];
  const valid = picked.length === hours && picked.every((s) => !s.taken && !s.past);
  const price = picked.reduce((a, s) => a + s.price, 0);
  const people = band.data?.members.length ?? 1;

  async function submit() {
    if (!roomId || start === null) return;
    setBusy(true);
    setError(null);
    try {
      await book(bandId, { roomId, date, startHour: start, hours, jamId });
      nav(`/bands/${bandId}`, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : '예약하지 못했어요');
      getSlots(roomId, date).then(setSlots).catch(() => undefined);
      setBusy(false);
    }
  }

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <button className="btn-ghost" onClick={() => (room ? setRoomId(null) : nav(-1))}>← {room ? '합주실 목록' : band.data?.title ?? '밴드'}</button>
        </div>
        <h1 className="h1">{room ? `${room.studio.name} ${room.name}` : '합주실 예약'}</h1>
        {!room && <p className="sub">예시 합주실이에요. 실제 합주실 예약과는 아직 연결되어 있지 않아요.</p>}
        {!room && (
          <div className="chips" role="group" aria-label="지역">
            <button className="chip dark" aria-pressed={area === null} onClick={() => setArea(null)}>전체</button>
            {areas.map((a) => <button key={a} className="chip dark" aria-pressed={area === a} onClick={() => setArea(a)}>{a}</button>)}
          </div>
        )}
      </header>

      {!room && (
        <div className="body pad">
          {studios.error && <p className="err-text">{studios.error}</p>}
          {shown.map((s) => (
            <section key={s.id} className="card studio">
              <div className="between"><b className="t">{s.name}</b><span className="sub">{hourLabel(s.openHour)}~{hourLabel(s.closeHour)}</span></div>
              <span className="sub">{s.station}</span>
              {s.perks.length > 0 && <div className="row wrap">{s.perks.map((p) => <span key={p} className="tag soft">{p}</span>)}</div>}
              {s.rooms.map((r) => (
                <button key={r.id} className="room" onClick={() => { setRoomId(r.id); setStart(q.get('hour') ? Number(q.get('hour')) : null); }}>
                  <span className="room-main"><b>{r.name} · 최대 {r.capacity}명</b><span className="sub">{r.gear}</span></span>
                  <span className="room-price">{won(r.pricePerHour)}<span className="sub">/시간</span></span>
                </button>
              ))}
            </section>
          ))}
        </div>
      )}

      {room && (
        <>
          <div className="body pad">
            <section className="card">
              <span className="sub">{room.studio.station} · {room.gear}</span>
              <div className="chips" role="group" aria-label="날짜">
                {Array.from({ length: 14 }, (_, i) => addDays(todayKst(), i)).map((d) => {
                  const dow = DAYS[(new Date(d + 'T00:00:00Z').getUTCDay() + 6) % 7];
                  return (
                    <button key={d} className="chip date" aria-pressed={date === d} onClick={() => { setDate(d); setStart(null); }}>
                      <span>{dow}</span><b>{Number(d.slice(8))}</b>
                    </button>
                  );
                })}
              </div>
            </section>
            <section className="card">
              <div className="between">
                <h2 className="section">시작 시간</h2>
                <div className="row" role="group" aria-label="이용 시간">
                  {[1, 2, 3, 4].map((h) => <button key={h} className="chip" aria-pressed={hours === h} onClick={() => setHours(h)}>{h}시간</button>)}
                </div>
              </div>
              {!slots && <p className="sub">시간표 불러오는 중…</p>}
              <div className="slot-grid">
                {slots?.slots.map((s) => {
                  const inPick = start !== null && s.hour >= start && s.hour < start + hours;
                  return (
                    <button key={s.hour} className={`slot-btn ${inPick ? 'pick' : ''}`} disabled={s.taken || s.past}
                            aria-pressed={start === s.hour} onClick={() => setStart(s.hour)}
                            aria-label={`${hourLabel(s.hour)} ${s.taken ? '예약됨' : won(s.price)}`}>
                      <b>{hourLabel(s.hour)}</b>
                      <span>{s.taken ? '예약됨' : s.past ? '지남' : won(s.price)}</span>
                    </button>
                  );
                })}
              </div>
              {start !== null && slots && !valid && <p className="err-text">고른 시간 중에 이미 찬 칸이 있거나 영업시간을 넘어요.</p>}
            </section>
          </div>
          <div className="bar pad col">
            {error && <p className="err-text center-text" role="alert">{error}</p>}
            {valid && (
              <p className="sub center-text">
                {hourLabel(start!)}–{hourLabel(start! + hours)} · 총 {won(price)} · {people}명이면 1인 <b>{won(perPerson(price, people))}</b>
              </p>
            )}
            <button className="cta" disabled={!valid || busy || band.data?.me !== 'HOST'} onClick={submit}>
              {band.data && band.data.me !== 'HOST' ? '예약은 방장이 해요' : busy ? '예약하는 중' : '이 시간으로 예약'}
            </button>
          </div>
        </>
      )}
    </main>
  );
}
