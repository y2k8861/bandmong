# 1단계 — 뼈대, 로그인, 프로필

## 만든 것

| 기능 | API | 화면 |
| --- | --- | --- |
| 로그인 방법 목록 | `GET /api/auth/providers` | 로그인 |
| 개발용 로그인 (dev 프로필 전용) | `POST /api/dev/login` | 로그인 |
| 소셜 로그인 (운영) | `/oauth2/authorization/{kakao\|naver\|google}` | 로그인 |
| 내 정보 | `GET /api/me` | 앱 시작 시 |
| 프로필 저장 | `PUT /api/me/profile` | 온보딩 · 프로필 수정 |
| 로그아웃 | `POST /api/auth/logout` | 홈 |
| 서버 시각 | `GET /api/time` | (마감 판정용, 이후 단계) |

오류는 모두 `{ "message": "화면에 보여줄 문구" }` 한 가지 모양으로 돌려준다.

## 왜 이렇게 만들었나

### 1. JWT 대신 세션 + CSRF 쿠키
- 웹앱 하나만 쓰는 서비스라 서버가 세션을 들고 있어도 부담이 없다.
- 토큰을 localStorage에 두지 않으니 XSS가 생겨도 로그인 토큰을 통째로 빼 가기 어렵다.
- 대신 쿠키 기반이라 CSRF를 막아야 한다 → `XSRF-TOKEN` 쿠키를 프론트가 읽어 `X-XSRF-TOKEN` 헤더로 다시 보낸다(이중 제출).
- Spring Security 6은 토큰을 늦게 만들기 때문에, SPA가 첫 요청부터 쿠키를 받도록 `CsrfCookieFilter`로 미리 꺼내 둔다.
- 로그인 직후 `changeSessionId()`로 세션 id를 바꿔 세션 고정 공격을 막는다.

### 2. 로그인 방법이 달라도 principal은 "우리 회원 id" 하나
- 개발용·카카오·네이버·구글 어느 쪽으로 들어와도 `SessionLogin`이 세션에 `Long userId`만 넣는다.
- 그래서 컨트롤러는 `@AuthenticationPrincipal Long userId` 한 줄로 끝나고, 로그인 수단이 늘어도 비즈니스 코드는 그대로다.
- 회원 식별은 `(provider, provider_id)` 유니크 키. 같은 사람이 다른 소셜로 들어오면 다른 계정이다(계정 연결은 이후 과제).

### 3. 개발용 로그인은 운영에 "존재하지 않게"
- `@ConditionalOnProperty(bandmong.dev-login=true)` — dev 프로필에서만 컨트롤러 빈이 등록된다.
- `if (운영이면) return 403` 같은 코드가 아니라 아예 엔드포인트가 없으므로 실수로 열릴 일이 없다.

### 4. 소셜 닉네임을 그대로 쓰지 않음
- 카카오 닉네임은 길이·문자 제한이 우리 규칙(한글·영문·숫자 2~10자)과 다르다.
- 온보딩에서 직접 정하게 하고, 그 전까지는 `onboarded=false`로 밴드 기능을 막는다.

### 5. 검증은 양쪽에서, 최종 판정은 서버
- 프론트 `nicknameError()`와 서버 `@Pattern`이 같은 정규식을 쓴다 — 프론트는 빠른 안내, 서버는 진짜 방어.
- 서버 검증 실패 문구가 그대로 화면에 뜨도록 메시지를 한국어로 적어 두었다.

## 테스트

- 백엔드 `MeApiTest`, `AuthApiTest`: 로그인 전 401, 온보딩 전/후 상태, 닉네임·악기 검증 문구, CSRF 없는 쓰기 403, 로그아웃 후 세션 끊김, 소셜별 회원번호 추출.
- 프론트: 닉네임 규칙, CSRF 쿠키 읽기, 온보딩 버튼 활성화·저장 요청 모양·서버 오류 문구 표시.

## 다음 단계로 넘긴 것

- 운영 DB 스키마는 지금 `ddl-auto: update`로 만든다. 실제 데이터가 쌓이기 전에 **Flyway 마이그레이션**으로 바꾼다.
- 소셜 로그인 키 발급과 실제 연동 확인은 배포 단계에서.
