import { useState } from 'react';
import { Link } from 'react-router-dom';
import { exploreBands } from '../api/bands';
import type { Genre, Instrument, Me } from '../api/types';
import BandCard from '../components/BandCard';
import { GENRES } from '../lib/band';
import { INSTRUMENTS } from '../lib/profile';
import { useLoad } from '../lib/useLoad';

export default function ExplorePage({ me }: { me: Me }) {
  // 처음에는 "내 악기 자리가 빈 밴드"로 시작한다 (악기가 하나일 때)
  const [inst, setInst] = useState<Instrument | null>(me.instruments.length === 1 ? me.instruments[0] : null);
  const [genre, setGenre] = useState<Genre | null>(null);
  const [areaInput, setAreaInput] = useState('');
  const [area, setArea] = useState('');
  const { data, error, loading, reload } = useLoad(() => exploreBands({ genre, instrument: inst, area }), [genre, inst, area]);

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <h1 className="h1">밴드 찾기</h1>
          <Link className="btn-dark" to="/bands/new">+ 만들기</Link>
        </div>
        <form className="search" onSubmit={(e) => { e.preventDefault(); setArea(areaInput); }}>
          <input className="input" value={areaInput} placeholder="지역 (예: 강남, 판교)" aria-label="지역 검색"
                 onChange={(e) => setAreaInput(e.target.value)} />
          <button className="btn-dark">검색</button>
        </form>
        <div className="chips" role="group" aria-label="빈자리 악기">
          <button className="chip" aria-pressed={inst === null} onClick={() => setInst(null)}>모든 자리</button>
          {INSTRUMENTS.map((i) => (
            <button key={i.id} className="chip" aria-pressed={inst === i.id} onClick={() => setInst(i.id)}>
              {i.name}{me.instruments.includes(i.id) ? ' ★' : ''}
            </button>
          ))}
        </div>
        <div className="chips" role="group" aria-label="장르">
          <button className="chip dark" aria-pressed={genre === null} onClick={() => setGenre(null)}>모든 장르</button>
          {GENRES.map((g) => (
            <button key={g.id} className="chip dark" aria-pressed={genre === g.id} onClick={() => setGenre(g.id)}>{g.name}</button>
          ))}
        </div>
      </header>
      <div className="body pad">
        {error && <div className="card"><p className="err-text">{error}</p><button className="btn-ghost" onClick={reload}>다시 시도</button></div>}
        {data && <p className="sub" aria-live="polite">{loading ? '찾는 중…' : `빈자리 있는 밴드 ${data.length}곳`}</p>}
        {data?.map((b) => <BandCard key={b.id} band={b} />)}
        {data && data.length === 0 && !loading && (
          <section className="card empty">
            <b>조건에 맞는 밴드가 없어요</b>
            <p className="sub">조건을 줄이거나, 내가 먼저 밴드를 열어 사람을 모아 보세요.</p>
            <Link className="cta small" to="/bands/new">밴드 만들기</Link>
          </section>
        )}
      </div>
    </main>
  );
}
