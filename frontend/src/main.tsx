import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
// 글꼴을 앱에 싣는다 (iOS 앱은 오프라인에서도 같은 글꼴): 본문 IBM Plex Sans KR, 로고만 Black Han Sans
import '@fontsource/ibm-plex-sans-kr/400.css';
import '@fontsource/ibm-plex-sans-kr/700.css';
import '@fontsource/black-han-sans/400.css';
import './tokens.css';
import './base.css';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
