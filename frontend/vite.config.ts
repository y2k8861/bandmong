/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 중에는 /api, 소셜 로그인 경로를 스프링 서버(8080)로 넘긴다.
// 같은 출처처럼 보이므로 세션·CSRF 쿠키가 그대로 동작한다.
const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
    },
  },
  test: {
    environment: 'jsdom',
  },
});
