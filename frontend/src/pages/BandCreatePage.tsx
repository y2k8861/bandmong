import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createBand } from '../api/bands';
import type { Genre, Instrument, Level, Me, Seats } from '../api/types';
import SeatStepper from '../components/SeatStepper';
import { GENRES, seatError, seatTotal } from '../lib/band';
import { INSTRUMENTS, LEVELS } from '../lib/profile';

export default function BandCreatePage({ me }: { me: Me }) {
  const nav = useNavigate();
  const [hostInst, setHostInst] = useState<Instrument>(me.instruments[0]);
  const [seats, setSeats] = useState<Seats>(() => {
    const s: Seats = { VOCAL: 1, GUITAR: 1, BASS: 1, DRUM: 1 };
    s[me.instruments[0]] = Math.max(1, s[me.instruments[0]] ?? 0);
    return s;
  });
  const [title, setTitle] = useState('');
  const [genre, setGenre] = useState<Genre>('ROCK');
  const [level, setLevel] = useState<Level | null>(null);
  const [area, setArea] = useState(me.region ?? '');
  const [schedule, setSchedule] = useState('');
  const [intro, setIntro] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function pickHost(i: Instrument) {
    setHostInst(i);
    setSeats((s) => ({ ...s, [i]: Math.max(1, s[i] ?? 0) }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const err = !title.trim() ? '밴드 이름을 입력해 주세요'
      : !area.trim() ? '지역을 입력해 주세요'
      : seatTotal(seats) < 2 ? '함께할 자리를 하나 이상 만들어 주세요'
      : seatError(seats);
    if (err) return setError(err);
    setBusy(true);
    setError(null);
    try {
      const band = await createBand({ title: title.trim(), genre, level, area: area.trim(), schedule, intro, hostInstrument: hostInst, seats });
      nav(`/bands/${band.id}`, { replace: true });
    } catch (e2) {
      setError(e2 instanceof Error ? e2.message : '만들지 못했어요');
      setBusy(false);
    }
  }

  return (
    <main className="scr" style={{ background: 'var(--card)' }}>
      <header className="top pad row">
        <button className="btn-ghost" onClick={() => nav(-1)}>취소</button>
        <h1 className="h2">밴드 만들기</h1>
      </header>
      <form id="create" className="body pad form" onSubmit={submit} noValidate>
        <div className="field">
          <label className="label" htmlFor="title">밴드 이름</label>
          <input id="title" className="input" maxLength={24} placeholder="예: 금요일 밤 록 커버" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="field">
          <span className="label">장르</span>
          <div className="row wrap" role="group" aria-label="장르">
            {GENRES.map((g) => <button type="button" key={g.id} className="chip dark" aria-pressed={genre === g.id} onClick={() => setGenre(g.id)}>{g.name}</button>)}
          </div>
        </div>
        <div className="field">
          <span className="label">원하는 실력</span>
          <div className="row wrap" role="group" aria-label="원하는 실력">
            <button type="button" className="chip" aria-pressed={level === null} onClick={() => setLevel(null)}>상관없음</button>
            {LEVELS.map((l) => <button type="button" key={l.id} className="chip" aria-pressed={level === l.id} onClick={() => setLevel(l.id)}>{l.name}</button>)}
          </div>
        </div>
        {me.instruments.length > 1 && (
          <div className="field">
            <span className="label">내가 맡을 악기</span>
            <div className="row wrap" role="group" aria-label="내가 맡을 악기">
              {me.instruments.map((i) => (
                <button type="button" key={i} className="chip" aria-pressed={hostInst === i} onClick={() => pickHost(i)}>
                  {INSTRUMENTS.find((x) => x.id === i)?.name}
                </button>
              ))}
            </div>
          </div>
        )}
        <SeatStepper seats={seats} minimum={{ [hostInst]: 1 }} onChange={setSeats}
                     note={(i) => (i === hostInst ? '내 자리 포함' : null)} />
        <div className="two">
          <div className="field">
            <label className="label" htmlFor="area">지역</label>
            <input id="area" className="input" maxLength={20} placeholder="강남역 근처" value={area} onChange={(e) => setArea(e.target.value)} />
          </div>
          <div className="field">
            <label className="label" htmlFor="schedule">정기 모임 <span>선택</span></label>
            <input id="schedule" className="input" maxLength={20} placeholder="매주 금 20:00" value={schedule} onChange={(e) => setSchedule(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label className="label" htmlFor="intro">소개 <span>선택 · 300자</span></label>
          <textarea id="intro" className="input area" maxLength={300} rows={3} placeholder="어떤 곡을, 어떤 분위기로 하고 싶은지 적어 주세요"
                    value={intro} onChange={(e) => setIntro(e.target.value)} />
        </div>
        {error && <p className="err-text" role="alert">{error}</p>}
      </form>
      <div className="bar pad">
        <button className="cta" type="submit" form="create" disabled={busy}>{busy ? '여는 중' : '밴드 열기'}</button>
      </div>
    </main>
  );
}
