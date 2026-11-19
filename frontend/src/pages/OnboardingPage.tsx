import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveProfile } from '../api/auth';
import type { Instrument, Level, Me } from '../api/types';
import { INSTRUMENTS, LEVELS, nicknameError } from '../lib/profile';

export default function OnboardingPage({ me, onSaved }: { me: Me; onSaved: (me: Me) => void }) {
  const nav = useNavigate();
  const [inst, setInst] = useState<Instrument[]>(me.instruments);
  const [level, setLevel] = useState<Level | null>(me.level);
  const [nickname, setNickname] = useState(me.nickname ?? '');
  const [region, setRegion] = useState(me.region ?? '');
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nickErr = nicknameError(nickname);
  const ready = inst.length > 0 && level !== null && !nickErr;

  const toggle = (id: Instrument) =>
    setInst((cur) => (cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]));

  async function submit() {
    setTouched(true);
    if (!ready || !level) return;
    setBusy(true);
    setError(null);
    try {
      const saved = await saveProfile({ nickname: nickname.trim(), instruments: inst, level, region: region.trim() });
      onSaved(saved);
      nav('/', { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장하지 못했어요');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="scr">
      <div className="body onb pad">
        <h1 className="h1">{me.onboarded ? '프로필 수정' : '어떤 악기를 맡나요?'}</h1>

        <section className="field">
          <h2 className="label">악기 <span>여러 개 골라도 돼요</span></h2>
          <div className="inst-grid">
            {INSTRUMENTS.map((i) => (
              <button key={i.id} type="button" className="inst" aria-pressed={inst.includes(i.id)} onClick={() => toggle(i.id)}>
                <b>{i.name}</b><span>{i.sub}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="field">
          <h2 className="label">실력</h2>
          <div className="lv-grid">
            {LEVELS.map((l) => (
              <button key={l.id} type="button" className="lv" aria-pressed={level === l.id} onClick={() => setLevel(l.id)}>
                {l.name}
              </button>
            ))}
          </div>
        </section>

        <section className="field">
          <label className="label" htmlFor="nick">닉네임 <span>한글·영문·숫자 2~10자</span></label>
          <input id="nick" className="input" value={nickname} maxLength={10} placeholder="예: 퇴근베이스"
                 aria-invalid={touched && !!nickErr} onChange={(e) => setNickname(e.target.value)} onBlur={() => setTouched(true)} />
          {touched && nickErr && <p className="err-text">{nickErr}</p>}
        </section>

        <section className="field">
          <label className="label" htmlFor="region">주로 합주할 동네 <span>선택</span></label>
          <input id="region" className="input" value={region} maxLength={30} placeholder="예: 마포구, 강남역"
                 onChange={(e) => setRegion(e.target.value)} />
        </section>

        {error && <p className="err-text" role="alert">{error}</p>}
      </div>
      <div className="bar pad">
        <button className="cta" disabled={busy || !ready} onClick={submit}>
          {busy ? '저장하는 중' : me.onboarded ? '저장' : '밴드몽 시작하기'}
        </button>
      </div>
    </main>
  );
}
