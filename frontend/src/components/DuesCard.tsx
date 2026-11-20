import { useState } from 'react';
import { getDues, setDuesPaid, setDuesSettings } from '../api/studios';
import type { DuesView } from '../api/types';
import { useLoad } from '../lib/useLoad';
import { won } from '../lib/money';
import Sheet from './Sheet';

const thisMonth = () => new Date(Date.now() + 9 * 3600_000).toISOString().slice(0, 7);
const shift = (m: string, n: number) => {
  const [y, mo] = m.split('-').map(Number);
  const d = new Date(Date.UTC(y, mo - 1 + n, 1));
  return d.toISOString().slice(0, 7);
};

/** 밴드 상세: 월 회비 */
export default function DuesCard({ bandId, myId }: { bandId: number; myId: number }) {
  const [month, setMonth] = useState(thisMonth());
  const { data, setData, error } = useLoad(() => getDues(bandId, month), [bandId, month]);
  const [edit, setEdit] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  async function run(fn: () => Promise<DuesView>) {
    setMsg(null);
    try { setData(await fn()); } catch (e) { setMsg(e instanceof Error ? e.message : '처리하지 못했어요'); }
  }

  const label = `${Number(month.slice(5))}월`;
  return (
    <section className="card">
      <div className="between">
        <h2 className="section">회비</h2>
        {data?.host && <button className="btn-ghost" onClick={() => setEdit(true)}>{data.amount ? '바꾸기' : '회비 정하기'}</button>}
      </div>
      {error && <p className="err-text">{error}</p>}
      {data && !data.amount && <p className="sub">정해진 회비가 없어요.{data.host ? ' 합주실비 말고 매달 걷는 돈이 있으면 정해 두세요.' : ''}</p>}
      {data?.amount && (
        <>
          <div className="between">
            <div className="row">
              <button className="btn-ghost" aria-label="이전 달" onClick={() => setMonth(shift(month, -1))}>‹</button>
              <b>{label} {won(data.amount)}</b>
              <button className="btn-ghost" aria-label="다음 달" disabled={month >= shift(thisMonth(), 1)} onClick={() => setMonth(shift(month, 1))}>›</button>
            </div>
            <span className="sub">{data.paidCount}/{data.total}명 냄</span>
          </div>
          {data.account && <p className="sub">보낼 곳: {data.account}</p>}
          <div className="row wrap">
            {data.members.map((m) => (
              <button key={m.userId} className={`settle ${m.paid ? 'paid' : ''}`} disabled={!data.host && m.userId !== myId}
                      aria-pressed={m.paid} onClick={() => run(() => setDuesPaid(bandId, data.month, m.userId, !m.paid))}>
                {m.paid ? '✓ ' : ''}{m.nickname}
              </button>
            ))}
          </div>
        </>
      )}
      {msg && <p className="err-text" role="alert">{msg}</p>}
      {edit && data && (
        <DuesSheet initial={data} onClose={() => setEdit(false)}
                   onSave={async (amount, account) => { await run(() => setDuesSettings(bandId, amount, account)); setEdit(false); }} />
      )}
    </section>
  );
}

function DuesSheet({ initial, onClose, onSave }: { initial: DuesView; onClose: () => void; onSave: (a: number, acc: string) => void }) {
  const [amount, setAmount] = useState(String(initial.amount ?? ''));
  const [account, setAccount] = useState(initial.account ?? '');
  const n = Number(amount.replace(/[^0-9]/g, '') || 0);
  return (
    <Sheet title="월 회비" onClose={onClose}>
      <div className="field">
        <label className="label" htmlFor="dues-amt">한 달 금액 <span>0이면 회비 없음</span></label>
        <input id="dues-amt" className="input" inputMode="numeric" value={amount} placeholder="30000"
               onChange={(e) => setAmount(e.target.value.replace(/[^0-9]/g, ''))} />
        {n > 0 && <span className="sub">{won(n)}</span>}
      </div>
      <div className="field">
        <label className="label" htmlFor="dues-acc">보낼 곳 <span>선택</span></label>
        <input id="dues-acc" className="input" maxLength={60} value={account} placeholder="예: 카카오뱅크 3333-01-1234567 김방장"
               onChange={(e) => setAccount(e.target.value)} />
      </div>
      <button className="cta" disabled={n > 1_000_000} onClick={() => onSave(n, account)}>저장</button>
    </Sheet>
  );
}
