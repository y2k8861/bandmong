import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { acceptApp, applyBand, cancelApply, getBand, leaveBand, rejectApp, updateBand } from '../api/bands';
import type { BandDetail, Instrument, Me, Seats } from '../api/types';
import BandJams from '../components/BandJams';
import BookingsCard from '../components/BookingsCard';
import DuesCard from '../components/DuesCard';
import GigsCard from '../components/GigsCard';
import SeatStepper from '../components/SeatStepper';
import Sheet from '../components/Sheet';
import ReportSheet from '../components/ReportSheet';
import { transferHost } from '../api/moderation';
import { ago, genreName, openCount, seatError } from '../lib/band';
import { LEVELS, instrumentName } from '../lib/profile';
import { useLoad } from '../lib/useLoad';

const levelName = (l: string | null) => LEVELS.find((x) => x.id === l)?.name;

export default function BandDetailPage({ me }: { me: Me }) {
  const id = Number(useParams().id);
  const nav = useNavigate();
  const { data: band, setData, error, loading, reload } = useLoad(() => getBand(id), [id]);
  const [sheet, setSheet] = useState<null | 'apply' | 'seats' | 'leave' | 'report'>(null);
  const [member, setMember] = useState<{ userId: number; nickname: string } | null>(null);
  const [reportUser, setReportUser] = useState<{ userId: number; nickname: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  /** 서버 응답(새 상세)으로 화면을 바꾸고, 실패하면 서버 문구를 보여준 뒤 최신 상태로 다시 읽는다 */
  async function act(fn: () => Promise<BandDetail | void>, done?: string) {
    setBusy(true);
    try {
      const next = await fn();
      if (next) setData(next);
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

  if (loading && !band) return <main className="scr center"><p className="sub">불러오는 중…</p></main>;
  if (error && !band) {
    return (
      <main className="scr center pad">
        <p className="err-text">{error}</p>
        <button className="btn-ghost" onClick={() => nav('/explore')}>찾기로 돌아가기</button>
      </main>
    );
  }
  if (!band) return null;

  const host = band.members.find((m) => m.role === 'HOST');
  const canApply = band.recruiting && me.instruments.some((i) => openCount(band.seats, i) > 0);

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <button className="btn-ghost" onClick={() => nav(-1)}>← 뒤로</button>
          {band.me === 'HOST'
            ? <button className="btn-ghost" onClick={() => setSheet('seats')}>자리·모집 설정</button>
            : <button className="btn-ghost" onClick={() => setSheet('report')}>밴드 신고</button>}
        </div>
        <div className="row"><span className="genre">{genreName(band.genre)}</span>{!band.recruiting && <span className="tag soft">모집 마감</span>}</div>
        <h1 className="h1">{band.title}</h1>
        <p className="sub">
          {band.area}{band.schedule && ` · ${band.schedule}`} · 실력 {levelName(band.level) ?? '상관없음'} · 방장 {host?.nickname}
        </p>
      </header>

      <div className="body pad">
        {band.intro && <section className="card"><p className="intro">{band.intro}</p></section>}

        {(band.me === 'HOST' || band.me === 'MEMBER') && (
          <Link to={`/bands/${band.id}/chat`} className="card chat-link">
            <span><b>밴드 채팅</b><span className="sub">멤버끼리 이야기해요</span></span>
            {(band.unreadMessages ?? 0) > 0 ? <span className="badge chat">새 메시지 {band.unreadMessages}</span> : <span className="sub">열기 →</span>}
          </Link>
        )}

        {(band.me === 'HOST' || band.me === 'MEMBER') && (
          <>
            <BandJams bandId={band.id} isHost={band.me === 'HOST'} />
            <BookingsCard bandId={band.id} isHost={band.me === 'HOST'} />
            <GigsCard bandId={band.id} isHost={band.me === 'HOST'} />
            <DuesCard bandId={band.id} myId={me.id} />
          </>
        )}

        <section className="card">
          <h2 className="section">자리 <span className="mono">{band.members.length}/{band.seats.reduce((a, s) => a + s.total, 0)}</span></h2>
          <ul className="slots">
            {band.seats.flatMap((s) => {
              const people = band.members.filter((m) => m.instrument === s.instrument);
              const rows = people.map((m) => (
                <li key={`m${m.userId}`} className="slot">
                  <span className="inst-name">{instrumentName(s.instrument)}</span>
                  <b>{m.nickname}{m.userId === me.id && ' (나)'}</b>
                  {m.role === 'HOST' && <span className="tag">방장</span>}
                  <span className="sub push">{levelName(m.level)}</span>
                  {m.userId !== me.id && (
                    <button className="more" aria-label={`${m.nickname} 더 보기`} onClick={() => setMember({ userId: m.userId, nickname: m.nickname })}>⋯</button>
                  )}
                </li>
              ));
              for (let k = people.length; k < s.total; k++) {
                rows.push(
                  <li key={`o${s.instrument}${k}`} className="slot open">
                    <span className="inst-name">{instrumentName(s.instrument)}</span>
                    <span className="sub">{band.recruiting ? '모집 중' : '빈자리'}</span>
                  </li>,
                );
              }
              return rows;
            })}
          </ul>
        </section>

        {band.me === 'HOST' && (
          <section className="card">
            <h2 className="section">지원자 <span className="mono">{band.applications?.length ?? 0}</span></h2>
            {(band.applications ?? []).length === 0 && <p className="sub">아직 지원한 사람이 없어요.</p>}
            {band.applications?.map((a) => {
              const full = openCount(band.seats, a.instrument) === 0;
              return (
                <article key={a.id} className="applicant">
                  <div className="between">
                    <b>{a.nickname}</b>
                    <span className="sub">{ago(a.createdAt)}</span>
                  </div>
                  <span className="sub">
                    {instrumentName(a.instrument)} 자리 지원 · {levelName(a.level)}{a.region && ` · ${a.region}`}
                  </span>
                  {a.message && <p className="quote">“{a.message}”</p>}
                  {full && <p className="err-text">{instrumentName(a.instrument)} 자리가 다 찼어요. 자리를 늘려야 수락할 수 있어요.</p>}
                  <div className="row">
                    <button className="btn-ghost" disabled={busy} onClick={() => act(() => rejectApp(band.id, a.id), `${a.nickname}님 지원을 거절했어요`)}>거절</button>
                    <button className="cta small" disabled={busy || full} onClick={() => act(() => acceptApp(band.id, a.id), `${a.nickname}님이 멤버가 됐어요`)}>수락</button>
                  </div>
                </article>
              );
            })}
          </section>
        )}

        {band.me === 'MEMBER' && (
          <button className="btn-ghost wide" onClick={() => setSheet('leave')}>밴드 나가기</button>
        )}
      </div>

      {(band.me === 'NONE' || band.me === 'PENDING') && (
        <div className="bar pad">
          {band.me === 'PENDING' && band.myApplication ? (
            <div className="pending">
              <div>
                <b>{instrumentName(band.myApplication.instrument)} 자리로 지원했어요</b>
                <p className="sub">방장이 확인하면 내 밴드에 나타나요.</p>
              </div>
              <button className="btn-ghost" disabled={busy} onClick={() => act(() => cancelApply(band.id), '지원을 취소했어요')}>지원 취소</button>
            </div>
          ) : (
            <button className="cta" disabled={!canApply} onClick={() => setSheet('apply')}>
              {!band.recruiting ? '모집 마감' : canApply ? '지원하기' : '내 악기 자리가 없어요'}
            </button>
          )}
        </div>
      )}

      {sheet === 'apply' && (
        <ApplySheet band={band} me={me} busy={busy} onClose={() => setSheet(null)}
                    onSubmit={(inst, msg) => act(() => applyBand(band.id, inst, msg), '지원했어요. 방장의 답을 기다려 주세요')} />
      )}
      {sheet === 'seats' && (
        <SeatsSheet band={band} busy={busy} onClose={() => setSheet(null)}
                    onSave={(patch) => act(() => updateBand(band.id, patch), '설정을 저장했어요')} />
      )}
      {sheet === 'leave' && (
        <Sheet title="밴드에서 나갈까요?" onClose={() => setSheet(null)}>
          <p className="sub">내 자리는 다시 빈자리가 돼요. 다시 들어오려면 새로 지원해야 해요.</p>
          <button className="cta danger" disabled={busy}
                  onClick={() => act(async () => { await leaveBand(band.id); nav('/', { replace: true }); }, '밴드에서 나왔어요')}>
            나가기
          </button>
        </Sheet>
      )}
      {member && (
        <Sheet title={member.nickname} onClose={() => setMember(null)}>
          {band.me === 'HOST' && (
            <button className="btn-dark wide" disabled={busy}
                    onClick={() => act(() => transferHost(band.id, member.userId), `${member.nickname}님이 새 방장이 됐어요`).then(() => setMember(null))}>
              방장 넘기기
            </button>
          )}
          {band.me === 'HOST' && <p className="sub">넘기면 지원자 수락·합주 확정·예약을 {member.nickname}님이 맡고, 나는 일반 멤버가 돼요.</p>}
          <button className="btn-ghost wide danger-text" onClick={() => { setReportUser(member); setMember(null); }}>{member.nickname}님 신고</button>
        </Sheet>
      )}
      {reportUser && <ReportSheet target="USER" id={reportUser.userId} label={`${reportUser.nickname}님`} onClose={() => setReportUser(null)} />}
      {sheet === 'report' && <ReportSheet target="BAND" id={band.id} label={`'${band.title}'`} onClose={() => setSheet(null)} />}
      {toast && <div className="toast" role="status">{toast}</div>}
    </main>
  );
}

function ApplySheet({ band, me, busy, onClose, onSubmit }: {
  band: BandDetail; me: Me; busy: boolean; onClose: () => void; onSubmit: (i: Instrument, msg: string) => void;
}) {
  const choices = me.instruments.filter((i) => openCount(band.seats, i) > 0);
  const [inst, setInst] = useState<Instrument>(choices[0]);
  const [msg, setMsg] = useState('');
  return (
    <Sheet title="지원하기" onClose={onClose}>
      <div className="field">
        <span className="label">어떤 자리로 지원할까요?</span>
        <div className="row wrap" role="group" aria-label="지원할 자리">
          {choices.map((i) => (
            <button key={i} type="button" className="chip" aria-pressed={inst === i} onClick={() => setInst(i)}>
              {instrumentName(i)} · {openCount(band.seats, i)}자리
            </button>
          ))}
        </div>
      </div>
      <div className="field">
        <label className="label" htmlFor="msg">방장에게 한마디 <span>선택</span></label>
        <textarea id="msg" className="input area" rows={3} maxLength={200} value={msg}
                  placeholder="예: 평일 저녁 주 1회 가능해요. 데이식스 좋아해요!" onChange={(e) => setMsg(e.target.value)} />
      </div>
      <button className="cta" disabled={busy || !inst} onClick={() => onSubmit(inst, msg)}>{busy ? '보내는 중' : '지원 보내기'}</button>
    </Sheet>
  );
}

function SeatsSheet({ band, busy, onClose, onSave }: {
  band: BandDetail; busy: boolean; onClose: () => void;
  onSave: (p: { seats: Seats; recruiting: boolean; schedule: string; intro: string }) => void;
}) {
  const filled: Seats = {};
  band.seats.forEach((s) => { filled[s.instrument] = s.filled; });
  const [seats, setSeats] = useState<Seats>(() => Object.fromEntries(band.seats.map((s) => [s.instrument, s.total])));
  const [recruiting, setRecruiting] = useState(band.recruiting);
  const [schedule, setSchedule] = useState(band.schedule ?? '');
  const [intro, setIntro] = useState(band.intro ?? '');
  const err = seatError(seats, filled);
  return (
    <Sheet title="자리·모집 설정" onClose={onClose}>
      <label className="switch">
        <input type="checkbox" checked={recruiting} onChange={(e) => setRecruiting(e.target.checked)} />
        <span><b>멤버 모집 중</b><span className="sub">끄면 찾기 목록에서 빠지고 새 지원을 받지 않아요</span></span>
      </label>
      <SeatStepper seats={seats} minimum={filled} onChange={setSeats}
                   note={(i) => ((filled[i] ?? 0) > 0 ? `${filled[i]}명 활동 중` : null)} />
      <div className="field">
        <label className="label" htmlFor="sch">정기 모임</label>
        <input id="sch" className="input" maxLength={20} value={schedule} onChange={(e) => setSchedule(e.target.value)} />
      </div>
      <div className="field">
        <label className="label" htmlFor="intro2">소개</label>
        <textarea id="intro2" className="input area" rows={3} maxLength={300} value={intro} onChange={(e) => setIntro(e.target.value)} />
      </div>
      {err && <p className="err-text">{err}</p>}
      <button className="cta" disabled={busy || !!err} onClick={() => onSave({ seats, recruiting, schedule, intro })}>저장</button>
    </Sheet>
  );
}
