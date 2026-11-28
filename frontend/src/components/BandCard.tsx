import { Link } from 'react-router-dom';
import type { BandSummary } from '../api/types';
import { genreName, openSeats } from '../lib/band';
import { instrumentName, LEVELS } from '../lib/profile';
import { answerName, dday, fmtDateTime, fmtTime } from '../lib/time';
import Facts from './Facts';

/** 밴드 카드. 다음 합주는 표 한 장처럼: 날짜, 크게 쓴 시각, D-day, 내 답. */
export default function BandCard({ band }: { band: BandSummary }) {
  const open = openSeats(band.seats);
  const jam = band.nextJam;
  return (
    <Link to={`/bands/${band.id}`} className="card band-card">
      <div className="band-card__head">
        <b className="t">{band.title}</b>
        <span className="band-card__flags">
          {band.me === 'HOST' && (band.pendingCount ?? 0) > 0 && <span className="badge">지원 {band.pendingCount}</span>}
          {band.me === 'PENDING' && <span className="badge soft">답 기다리는 중</span>}
          {(band.unreadMessages ?? 0) > 0 && <span className="badge chat">새 메시지 {band.unreadMessages}</span>}
        </span>
      </div>
      <Facts items={[
        <span key="g" className="genre">{genreName(band.genre)}</span>,
        band.area,
        band.schedule,
        band.level && LEVELS.find((l) => l.id === band.level)?.name,
      ]} />
      {jam && (
        <div className="ticket">
          <span className="ticket__when">
            <span className="ticket__date">{fmtDateTime(jam.startsAt).replace(/ \d\d:\d\d$/, '')}</span>
            <span className="ticket__time">{fmtTime(jam.startsAt)}</span>
          </span>
          <span className="ticket__place">{jam.place ?? '장소 미정'}</span>
          <span className="ticket__side">
            <span className="dday">{dday(jam.startsAt)}</span>
            <span className={`my-ans ${jam.myAnswer ? jam.myAnswer.toLowerCase() : 'none'}`}>{answerName(jam.myAnswer)}</span>
          </span>
        </div>
      )}
      <div className="band-card__seats">
        {band.myInstrument && (
          <span className="tag">{band.me === 'HOST' ? `방장, ${instrumentName(band.myInstrument)}` : instrumentName(band.myInstrument)}</span>
        )}
        {open.map((s) => (
          <span key={s.instrument} className="tag open">{instrumentName(s.instrument)} {s.total - s.filled}자리</span>
        ))}
        {open.length === 0 && <span className="tag soft">자리 다 참</span>}
        <span className="sub">멤버 {band.memberCount}명</span>
      </div>
    </Link>
  );
}
