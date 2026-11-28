import { useState } from 'react';
import { Link } from 'react-router-dom';
import { listJams } from '../api/jams';
import type { JamView } from '../api/types';
import { answerName, dday, fmtDateTime, fmtTime } from '../lib/time';
import { useLoad } from '../lib/useLoad';
import Facts from './Facts';

/** 밴드 상세(멤버용)의 합주 섹션: 예정 합주 + 가능 시간 투표 바로가기 + 지난 합주 */
export default function BandJams({ bandId, isHost }: { bandId: number; isHost: boolean }) {
  const { data, error } = useLoad(() => listJams(bandId), [bandId]);
  const [showPast, setShowPast] = useState(false);

  return (
    <section className="card">
      <div className="between">
        <h2 className="section">합주</h2>
        <Link className="btn-dark" to={`/bands/${bandId}/schedule`}>{isHost ? '일정 잡기' : '가능 시간 투표'}</Link>
      </div>
      {error && <p className="err-text">{error}</p>}
      {data && data.upcoming.length === 0 && (
        <p className="sub">잡힌 합주가 없어요. {isHost ? '멤버들 가능 시간을 모아 확정해 보세요.' : '가능 시간을 내면 방장이 확정해요.'}</p>
      )}
      {data?.upcoming.map((j) => <JamRow key={j.id} jam={j} />)}
      {data && data.past.length > 0 && (
        <>
          <button className="link-btn" onClick={() => setShowPast((v) => !v)}>
            지난 합주 {data.past.length}번 {showPast ? '접기' : '보기'}
          </button>
          {showPast && data.past.map((j) => <JamRow key={j.id} jam={j} />)}
        </>
      )}
    </section>
  );
}

function JamRow({ jam }: { jam: JamView }) {
  const canceled = jam.status === 'CANCELED';
  return (
    <Link to={`/jams/${jam.id}`} className={`jam-row ${canceled ? 'canceled' : ''}`}>
      <span className="dday">{canceled ? '취소' : jam.over ? '지남' : dday(jam.startsAt)}</span>
      <span className="jam-main">
        <b>{fmtDateTime(jam.startsAt)}~{fmtTime(jam.endsAt)}</b>
        <span className="sub"><Facts items={[jam.place ?? '장소 미정', `참석 ${jam.counts.yes}`, jam.counts.late ? `늦음 ${jam.counts.late}` : null]} /></span>
      </span>
      {!canceled && !jam.over && <span className={`my-ans ${jam.myAnswer ? jam.myAnswer.toLowerCase() : 'none'}`}>{answerName(jam.myAnswer)}</span>}
    </Link>
  );
}
