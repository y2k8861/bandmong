import { useState, type FormEvent } from 'react';
import { devLogin } from '../api/auth';
import { appDevLogin, type Issued } from '../api/app';
import { apiBase, isApp, setToken } from '../lib/platform';
import { signInWithApple, signInWithProvider } from '../lib/native';
import type { Me, Providers } from '../api/types';

const SOCIAL: Record<string, { label: string; className: string }> = {
  kakao: { label: '카카오로 시작하기', className: 'kakao' },
  naver: { label: '네이버로 시작하기', className: 'naver' },
  google: { label: 'Google로 시작하기', className: 'google' },
};

export default function LoginPage({ providers, onLogin }: { providers: Providers; onLogin: (me: Me) => void }) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** 앱 로그인 공통: 토큰을 저장하고 내 정보를 넘긴다 */
  async function finishApp(p: Promise<Issued>): Promise<Me> {
    const issued = await p;
    await setToken(issued.token);
    return issued.me;
  }

  async function appSignIn(how: () => Promise<Issued>) {
    setBusy(true);
    setError(null);
    try {
      onLogin(await finishApp(how()));
    } catch (err) {
      const msg = err instanceof Error ? err.message : '';
      // 사용자가 Apple 창을 닫은 경우는 조용히
      if (!/cancel|1001|취소/i.test(msg)) setError(msg || '로그인하지 못했어요');
    } finally {
      setBusy(false);
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return setError('이름을 입력해 주세요');
    setBusy(true);
    setError(null);
    try {
      onLogin(isApp ? await finishApp(appDevLogin(name.trim())) : await devLogin(name.trim()));
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인하지 못했어요');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="scr dark login pad">
      <div className="hero">
        <div className="eq" aria-hidden><span /><span /><span /><span /><span /></div>
        <h1 className="logo-xl">퇴근하고<br /><em>밴드몽</em></h1>
        <p>회사 근처에서 같이 합주할 사람을 찾고,<br />합주 일정과 밴드 살림을 한곳에서.</p>
      </div>

      <div className="social">
        {isApp && (
          <button className="social-btn apple" disabled={busy} onClick={() => appSignIn(signInWithApple)}>
             Apple로 계속하기
          </button>
        )}
        {providers.providers.map((id) => isApp ? (
          <button key={id} className={`social-btn ${SOCIAL[id]?.className ?? ''}`} disabled={busy}
                  onClick={() => appSignIn(() => signInWithProvider(id))}>
            {SOCIAL[id]?.label ?? id}
          </button>
        ) : (
          <a key={id} className={`social-btn ${SOCIAL[id]?.className ?? ''}`} href={`/oauth2/authorization/${id}`}>
            {SOCIAL[id]?.label ?? id}
          </a>
        ))}
        {isApp && error && !providers.devLogin && <p className="err-text" role="alert">{error}</p>}

        {providers.devLogin && (
          <form className="dev" onSubmit={submit}>
            <label htmlFor="dev-name">개발용 로그인 <span>이름만 넣으면 그 이름의 테스트 계정으로 들어가요</span></label>
            <div className="row">
              <input id="dev-name" value={name} maxLength={20} placeholder="예: 민지" onChange={(e) => setName(e.target.value)} />
              <button className="cta small" disabled={busy}>{busy ? '들어가는 중' : '들어가기'}</button>
            </div>
            {error && <p className="err-text" role="alert">{error}</p>}
          </form>
        )}

        {!providers.devLogin && providers.providers.length === 0 && (
          <p className="err-text">지금은 로그인 방법이 설정되어 있지 않아요</p>
        )}
        <p className="fine">
          시작하면 <a href={`${apiBase}/terms.html`} target="_blank" rel="noopener">이용약관</a>과{' '}
          <a href={`${apiBase}/privacy.html`} target="_blank" rel="noopener">개인정보 처리방침</a>에 동의하게 돼요
          {providers.supportEmail && <><br />문의 {providers.supportEmail}</>}
        </p>
      </div>
    </main>
  );
}
