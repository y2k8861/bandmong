import { useState } from 'react';
import type { JamInput } from '../api/types';
import { fromKstInput, toKstInput } from '../lib/time';
import Sheet from './Sheet';

const DURATIONS = [60, 90, 120, 180];

/** 합주 확정·수정 공용 시트. 실패하면 서버 문구를 시트 안에 보여준다. */
export default function JamFormSheet({ title, initial, submitLabel, onClose, onSubmit }: {
  title: string;
  initial: JamInput;
  submitLabel: string;
  onClose: () => void;
  onSubmit: (v: JamInput) => Promise<void>;
}) {
  const start = toKstInput(initial.startsAt);
  const [date, setDate] = useState(start.date);
  const [time, setTime] = useState(start.time);
  const [dur, setDur] = useState(initial.durationMinutes);
  const [place, setPlace] = useState(initial.place);
  const [memo, setMemo] = useState(initial.memo);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      await onSubmit({ startsAt: fromKstInput(date, time), durationMinutes: dur, place: place.trim(), memo: memo.trim() });
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장하지 못했어요');
      setBusy(false);
    }
  }

  return (
    <Sheet title={title} onClose={onClose}>
      <div className="two">
        <div className="field">
          <label className="label" htmlFor="jd">날짜</label>
          <input id="jd" className="input" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div className="field">
          <label className="label" htmlFor="jt">시작</label>
          <input id="jt" className="input" type="time" step={1800} value={time} onChange={(e) => setTime(e.target.value)} />
        </div>
      </div>
      <div className="field">
        <span className="label">길이</span>
        <div className="row wrap" role="group" aria-label="합주 길이">
          {DURATIONS.map((m) => (
            <button key={m} type="button" className="chip" aria-pressed={dur === m} onClick={() => setDur(m)}>
              {m % 60 ? `${Math.floor(m / 60)}시간 반` : `${m / 60}시간`}
            </button>
          ))}
        </div>
      </div>
      <div className="field">
        <label className="label" htmlFor="jp">장소 <span>선택</span></label>
        <input id="jp" className="input" maxLength={40} placeholder="예: 합정 그루브 합주실 A룸" value={place} onChange={(e) => setPlace(e.target.value)} />
      </div>
      <div className="field">
        <label className="label" htmlFor="jm">메모 <span>선택</span></label>
        <textarea id="jm" className="input area" rows={2} maxLength={200} placeholder="예: 셋리스트 3곡 맞춰 보기" value={memo} onChange={(e) => setMemo(e.target.value)} />
      </div>
      {error && <p className="err-text" role="alert">{error}</p>}
      <button className="cta" disabled={busy || !date || !time} onClick={submit}>{busy ? '저장하는 중' : submitLabel}</button>
    </Sheet>
  );
}
