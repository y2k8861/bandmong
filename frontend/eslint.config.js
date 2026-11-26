// 프론트 규칙. 실패 메시지에 "고치는 법"을 적어 둬서, 사람이든 AI든 메시지만 보고 고칠 수 있게 한다.
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

const only = (file, rule) => ({ ...rule, ignores: [file] });

const rules = {
  fetch: only('src/api/client.ts', {
    selector: "CallExpression[callee.name='fetch']",
    message: 'API는 src/api/*.ts 의 함수로 부르세요 (request()가 웹 CSRF·앱 토큰·서버 주소를 처리). 새 API면 해당 파일에 함수를 추가하세요.',
  }),
  eventSource: only('src/lib/stream.tsx', {
    selector: "NewExpression[callee.name='EventSource']",
    message: '실시간 연결은 lib/stream.tsx 하나만 엽니다. useStreamEvent(이벤트, fn)으로 구독하세요.',
  }),
  kst: only('src/lib/time.ts', {
    selector: "BinaryExpression[operator='*'][left.value=9][right.value=3600000]",
    message: '한국 시간 계산은 lib/time.ts 의 todayKst()·thisMonthKst()·thisMonday() 등을 쓰세요 (없으면 거기에 추가하고 테스트도 추가).',
  }),
  dialog: {
    selector: "CallExpression[callee.name=/^(alert|confirm|prompt)$/], CallExpression[callee.object.name='window'][callee.property.name=/^(alert|confirm|prompt)$/]",
    message: '브라우저 기본 창 대신 components/Sheet 를 쓰세요 (iOS 앱에서는 제목에 주소가 떠서 어색함).',
  },
};

const syntaxRules = (file) => ({
  files: file ? [file] : ['src/**/*.{ts,tsx}'],
  rules: {
    'no-restricted-syntax': ['error', ...Object.values(rules)
      .filter((r) => !(r.ignores ?? []).includes(file))
      .map(({ selector, message }) => ({ selector, message }))],
  },
});

export default tseslint.config(
  { ignores: ['dist', 'ios', 'node_modules', 'public', '*.config.*'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-restricted-globals': ['error',
        { name: 'localStorage', message: '저장은 lib/platform.ts 를 거치세요 (앱은 Preferences, 웹은 세션 쿠키). 토큰을 localStorage에 두지 않습니다.' },
        { name: 'sessionStorage', message: '저장은 lib/platform.ts 를 거치세요.' },
      ],
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['@capacitor/*', '@capacitor-community/*', '@capacitor-firebase/*', 'firebase', 'firebase/*'],
          message: '네이티브 기능은 lib/native.ts(또는 lib/platform.ts)에만 두고, 화면에서는 isApp으로 나눠 부르세요. 웹에서도 같은 코드가 돌아야 합니다.',
        }],
      }],
    },
  },
  // 같은 규칙 이름은 뒤 설정이 앞을 덮으므로, 파일마다 "자기 예외만 뺀 목록"을 한 번에 준다
  syntaxRules(),
  ...Object.values(rules).flatMap((r) => r.ignores ?? []).map((file) => syntaxRules(file)),
  {
    files: ['src/lib/native.ts', 'src/lib/platform.ts'],
    rules: { 'no-restricted-imports': 'off' },
  },
  {
    files: ['src/**/*.test.{ts,tsx}'],
    rules: { 'no-restricted-syntax': 'off' },
  },
);
