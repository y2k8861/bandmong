import { useState } from 'react';
import { Link } from 'react-router-dom';
import { adminReports, liftRestriction, resolveReport } from '../api/moderation';
import type { ModerationAction, ReportView } from '../api/types';
import { ago } from '../lib/band';
import { fmtDateTime } from '../lib/time';
import { useLoad } from '../lib/useLoad';

const TABS = [
  { id: 'OPEN', name: '처리 전' },
  { id: 'RESOLVED', name: '조치함' },
  { id: 'DISMISSED', name: '문제없음' },
] as const;

const TARGET = { USER: '회원', BAND: '밴드', MESSAGE: '메시지' } as const;

function actionsFor(r: ReportView): { id: ModerationAction; name: string; danger?: boolean }[] {
  const list: { id: ModerationAction; name: string; danger?: boolean }[] = [{ id: 'DISMISS', name: '문제없음' }];
  if (r.targetType === 'MESSAGE') list.push({ id: 'HIDE_MESSAGE', name: '메시지 가리기', danger: true });
  if (r.targetType === 'BAND') list.push({ id: 'CLOSE_RECRUITING', name: '모집 닫기', danger: true });
  list.push({ id: 'WARN', name: '경고', danger: true }, { id: 'SUSPEND_7', name: '7일 제한', danger: true }, { id: 'SUSPEND_30', name: '30일 제한', danger: true });
  return list;
}

export default function AdminPage() {
  const [tab, setTab] = useState<(typeof TABS)[number]['id']>('OPEN');
  const { data, setData, error, reload } = useLoad(() => adminReports(tab), [tab]);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [msg, setMsg] = useState<string | null>(null);

  async function act(r: ReportView, a: ModerationAction) {
    setMsg(null);
    try {
      await resolveReport(r.id, a, notes[r.id] ?? '');
      // 같은 대상 신고가 함께 닫히므로 목록에서 같은 대상을 모두 뺀다
      setData((cur) => cur && cur.filter((x) => !(x.targetType === r.targetType && x.targetId === r.targetId)));
      setMsg(`처리했어요 (${TARGET[r.targetType]} · ${a})`);
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '처리하지 못했어요');
      reload();
    }
  }

  async function lift(r: ReportView) {
    try { await liftRestriction(r.accusedId); setMsg(`${r.accusedNickname}님 제한을 풀었어요`); reload(); }
    catch (e) { setMsg(e instanceof Error ? e.message : '풀지 못했어요'); }
  }

  return (
    <main className="scr">
      <header className="top pad">
        <div className="between">
          <Link className="btn-ghost" to="/me">← 마이</Link>
          <span className="tag">관리자</span>
        </div>
        <h1 className="h1">신고 처리</h1>
        <div className="chips" role="group" aria-label="상태">
          {TABS.map((t) => <button key={t.id} className="chip dark" aria-pressed={tab === t.id} onClick={() => setTab(t.id)}>{t.name}</button>)}
        </div>
      </header>
      <div className="body pad">
        {msg && <p className="sub" role="status">{msg}</p>}
        {error && <p className="err-text">{error}</p>}
        {data && data.length === 0 && <section className="card empty"><b>{tab === 'OPEN' ? '처리할 신고가 없어요' : '기록이 없어요'}</b></section>}
        {data?.map((r) => (
          <article key={r.id} className="card report">
            <div className="between">
              <span className="row"><span className="tag soft">{TARGET[r.targetType]}</span><b>{r.reasonLabel}</b></span>
              <span className="sub">{ago(r.createdAt)}</span>
            </div>
            <p className="quote">{r.targetSummary}</p>
            {r.detail && <p className="sub">신고 내용: {r.detail}</p>}
            <p className="sub">
              신고 {r.reporterNickname} → 대상 <b>{r.accusedNickname ?? '(없음)'}</b>
              {' · '}이 대상 신고 {r.openOnTarget}건 · 이 사람 누적 {r.reportsAgainstAccused}건
            </p>
            {r.accusedRestrictedUntil && (
              <p className="err-text">
                {fmtDateTime(r.accusedRestrictedUntil)}까지 제한 중 <button className="link-btn" onClick={() => lift(r)}>제한 풀기</button>
              </p>
            )}
            {r.status === 'OPEN' ? (
              <>
                <input className="input" maxLength={300} placeholder="처리 메모 (선택, 운영 기록용)" aria-label="처리 메모"
                       value={notes[r.id] ?? ''} onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })} />
                <div className="row wrap">
                  {actionsFor(r).map((a) => (
                    <button key={a.id} className={a.danger ? 'btn-ghost danger-text' : 'btn-ghost'} onClick={() => act(r, a.id)}>{a.name}</button>
                  ))}
                </div>
              </>
            ) : (
              <p className="sub">결과: {r.resolution}{r.adminNote ? ` · ${r.adminNote}` : ''}</p>
            )}
          </article>
        ))}
      </div>
    </main>
  );
}
