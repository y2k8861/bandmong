import { useState } from 'react';
import { report } from '../api/moderation';
import type { ReportReason, ReportTarget } from '../api/types';
import Sheet from './Sheet';

const REASONS: { id: ReportReason; name: string }[] = [
  { id: 'ABUSE', name: '욕설·비하' },
  { id: 'SPAM', name: '광고·도배' },
  { id: 'NO_SHOW', name: '합주 노쇼·잠수' },
  { id: 'FAKE', name: '거짓 정보' },
  { id: 'SEXUAL', name: '성적인 내용' },
  { id: 'OTHER', name: '기타' },
];

/** 회원·밴드·메시지 신고. 보낸 뒤에는 결과 문구를 보여주고 닫는다. */
export default function ReportSheet({ target, id, label, onClose }: {
  target: ReportTarget; id: number; label: string; onClose: () => void;
}) {
  const [reason, setReason] = useState<ReportReason | null>(null);
  const [detail, setDetail] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!reason) return;
    setBusy(true);
    setError(null);
    try {
      setDone((await report(target, id, reason, detail.trim())).message);
    } catch (e) {
      setError(e instanceof Error ? e.message : '신고하지 못했어요');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Sheet title={`${label} 신고`} onClose={onClose}>
      {done ? (
        <>
          <p className="sub" role="status">{done}</p>
          <button className="cta" onClick={onClose}>닫기</button>
        </>
      ) : (
        <>
          <div className="reason-list" role="radiogroup" aria-label="신고 이유">
            {REASONS.map((r) => (
              <label key={r.id} className={`reason ${reason === r.id ? 'on' : ''}`}>
                <input type="radio" name="reason" checked={reason === r.id} onChange={() => setReason(r.id)} />
                {r.name}
              </label>
            ))}
          </div>
          <div className="field">
            <label className="label" htmlFor="rp-detail">자세히 <span>선택 · 500자</span></label>
            <textarea id="rp-detail" className="input area" rows={3} maxLength={500} value={detail}
                      placeholder="언제, 어떤 일이 있었는지 적어 주시면 빨리 확인할 수 있어요" onChange={(e) => setDetail(e.target.value)} />
          </div>
          <p className="sub">신고한 사실은 상대에게 알리지 않아요. 거짓 신고가 반복되면 이용이 제한될 수 있어요.</p>
          {error && <p className="err-text" role="alert">{error}</p>}
          <button className="cta danger" disabled={!reason || busy} onClick={submit}>{busy ? '보내는 중' : '신고하기'}</button>
        </>
      )}
    </Sheet>
  );
}
