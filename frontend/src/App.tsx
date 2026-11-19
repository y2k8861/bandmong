import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { getMe, getProviders } from './api/auth';
import { ApiError } from './api/client';
import type { Me, Providers } from './api/types';
import TabBar from './components/TabBar';
import LoginPage from './pages/LoginPage';
import OnboardingPage from './pages/OnboardingPage';
import MyBandsPage from './pages/MyBandsPage';
import ExplorePage from './pages/ExplorePage';
import BandCreatePage from './pages/BandCreatePage';
import BandDetailPage from './pages/BandDetailPage';
import MePage from './pages/MePage';

type Boot = { state: 'loading' } | { state: 'error'; message: string } | { state: 'ready'; providers: Providers; me: Me | null };

const withTabs = (page: ReactNode) => <>{page}<TabBar /></>;

export default function App() {
  const [boot, setBoot] = useState<Boot>({ state: 'loading' });

  const load = useCallback(async () => {
    try {
      // providers 요청이 CSRF 쿠키도 받아 온다. 그다음 로그인 여부 확인.
      const providers = await getProviders();
      let me: Me | null = null;
      try {
        me = await getMe();
      } catch (e) {
        if (!(e instanceof ApiError && e.status === 401)) throw e;
      }
      setBoot({ state: 'ready', providers, me });
    } catch {
      setBoot({ state: 'error', message: '서버에 연결하지 못했어요' });
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (boot.state === 'loading') {
    return <div className="app center"><div className="eq" aria-label="불러오는 중"><span /><span /><span /><span /></div></div>;
  }
  if (boot.state === 'error') {
    return (
      <div className="app center pad">
        <p className="err-text">{boot.message}</p>
        <button className="btn-dark" onClick={() => { setBoot({ state: 'loading' }); load(); }}>다시 시도</button>
      </div>
    );
  }

  const { me, providers } = boot;
  const setMe = (next: Me | null) => setBoot({ state: 'ready', providers, me: next });
  const home = !me ? '/login' : me.onboarded ? '/' : '/onboarding';

  return (
    <div className="app">
      <Routes>
        <Route path="/login" element={me ? <Navigate to={home} replace /> : <LoginPage providers={providers} onLogin={setMe} />} />
        <Route path="/onboarding" element={!me ? <Navigate to="/login" replace /> : <OnboardingPage me={me} onSaved={setMe} />} />
        {me?.onboarded ? (
          <>
            <Route path="/" element={withTabs(<MyBandsPage me={me} />)} />
            <Route path="/explore" element={withTabs(<ExplorePage me={me} />)} />
            <Route path="/me" element={withTabs(<MePage me={me} onLogout={() => setMe(null)} />)} />
            <Route path="/bands/new" element={<BandCreatePage me={me} />} />
            <Route path="/bands/:id" element={<BandDetailPage me={me} />} />
          </>
        ) : null}
        <Route path="*" element={<Navigate to={home} replace />} />
      </Routes>
    </div>
  );
}
