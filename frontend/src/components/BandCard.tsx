import { Link } from 'react-router-dom';
import type { BandSummary } from '../api/types';
import { genreName, openSeats } from '../lib/band';
import { instrumentName, LEVELS } from '../lib/profile';
import { answerName, dday, fmtDateTime } from '../lib/time';

export default function BandCard({ band }: { band: BandSummary }) {
  const open = openSeats(band.seats);
  return (
    <Link to={`/bands/${band.id}`} className="card band-card">
      <div className="between">
        <span className="genre">{genreName(band.genre)}</span>
        {band.me === 'HOST' && (band.pendingCount ?? 0) > 0 && <span className="badge">지원 {band.pendingCount}</span>}
        {band.me === 'PENDING' && <span className="badge soft">답 기다리는 중</span>}
      </div>
      <b className="t">{band.title}</b>
      <span className="sub">
        {band.area}
        {band.schedule && ` · ${band.schedule}`}
        {band.level && ` · ${LEVELS.find((l) => l.id === band.level)?.name}`}
      </span>
      {band.nextJam && (
        <div className="next-jam">
          <span className="dday">{dday(band.nextJam.startsAt)}</span>
          <span>{fmtDateTime(band.nextJam.startsAt)}{band.nextJam.place ? ` · ${band.nextJam.place}` : ''}</span>
          <span className={`my-ans ${band.nextJam.myAnswer ? band.nextJam.myAnswer.toLowerCase() : 'none'}`}>{answerName(band.nextJam.myAnswer)}</span>
        </div>
      )}
      <div className="row wrap">
        {band.myInstrument && (
          <span className="tag">{band.me === 'HOST' ? '방장 · ' : ''}{instrumentName(band.myInstrument)}</span>
        )}
        {open.map((s) => (
          <span key={s.instrument} className="tag open">{instrumentName(s.instrument)} {s.total - s.filled}자리</span>
        ))}
        {open.length === 0 && <span className="tag soft">자리 다 참</span>}
        <span className="sub push">멤버 {band.memberCount}명</span>
      </div>
    </Link>
  );
}
