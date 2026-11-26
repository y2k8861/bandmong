# 7단계: iOS 앱 출시 준비

## 무엇을 했나
- **Capacitor 7**로 지금의 React 화면을 iOS 앱으로 감쌌다 (`frontend/ios`, Bundle ID `com.bandmong.app`)
- **앱 전용 로그인**: Apple로 로그인, 카카오·네이버·구글은 시스템 브라우저 → `bandmong://` 로 복귀
- **푸시 알림**: Firebase Cloud Messaging, 알림을 누르면 해당 화면으로 이동
- **애플 심사 요건(1.2 사용자 콘텐츠)**: 욕설 필터, 사용자 차단, 앱 안 문의처
- **맥 없이 배포**: GitHub Actions macOS 머신에서 빌드·서명·TestFlight 업로드 (`.github/workflows/ios.yml`)
- 개인정보 처리방침·이용약관 초안 (`/privacy.html`, `/terms.html`)

## 결정과 이유

### 1. 앱은 쿠키 세션 대신 Bearer 토큰
앱 화면은 `capacitor://localhost`에서 열리고 API는 `https://도메인`에 있다. 출처가 달라서
iOS WebView(WKWebView)는 서버 쿠키를 서드파티로 보고 막는다 → 세션 로그인이 유지되지 않는다.

| | 웹 | 앱 |
|---|---|---|
| 인증 | 세션 쿠키 | `Authorization: Bearer <앱 토큰>` |
| CSRF | 쿠키 + 헤더 | 필요 없음 (쿠키를 안 쓰므로 위조 요청이 토큰을 실어 보낼 수 없음) |
| CORS | 같은 출처라 불필요 | `capacitor://localhost`만 허용, credentials 없음 |
| 실시간(SSE) | 쿠키 | `EventSource`는 헤더를 못 붙여서 `/api/stream`에서만 `?access_token=` 허용 |

- 토큰은 JWT가 아니라 **무작위 문자열**이고 서버에는 **SHA-256 해시만** 저장한다 (`app_tokens`).
  DB가 새도 토큰을 쓸 수 없고, 로그아웃·탈퇴 시 서버에서 바로 끊을 수 있다.
- 마지막 사용 시각은 매 요청이 아니라 **한 시간에 한 번만** 갱신해 쓰기 부하를 줄였다.
- 웹 흐름은 한 줄도 바꾸지 않아 기존 테스트가 그대로 통과한다. `AppTokenFilter`가 Bearer 요청에만 끼어든다.

### 2. 소셜 로그인 → 앱으로 돌아오기: 1회용 코드
앱이 `/oauth2/authorization/kakao?app=1`을 시스템 브라우저로 연다 → 로그인 성공 시 서버가
`bandmong://auth?code=…`로 보낸다 → 앱이 코드를 `/api/app/login/exchange`로 토큰과 바꾼다.
- URL에 토큰을 직접 싣지 않는다 (다른 앱이 같은 scheme을 가로챌 수 있음). 코드는 **2분, 한 번만** 유효.
- 소셜 개발자 콘솔의 Redirect URI는 웹과 같아서 바꿀 것이 없다.

### 3. Apple로 로그인 검증
앱이 받은 identityToken을 서버가 Apple 공개키(JWKS)로 검증한다: 서명, 발급자 `https://appleid.apple.com`,
대상(aud) = 우리 Bundle ID. 테스트에서는 검증기를 가짜로 바꿔(`@Primary`) 네트워크 없이 흐름을 확인한다.

### 4. 푸시는 "커밋 뒤"에, 설정이 없으면 조용히 끔
- 기존 `NotificationService`가 알림을 저장한 뒤 **트랜잭션 커밋 뒤에** SSE와 푸시를 함께 보낸다 → 롤백된 알림이 폰에 뜨지 않는다.
- 전송은 가상 스레드에서 해서 요청 응답을 늦추지 않는다. 만료된 토큰(UNREGISTERED)은 바로 지운다.
- `FCM_CREDENTIALS`가 비어 있으면 푸시를 끄고 나머지는 그대로 → 로컬·CI·데모 서버에 Firebase가 필요 없다.

### 5. 욕설 필터: 막을 곳과 가릴 곳을 나눴다
| 입력 | 처리 | 이유 |
|---|---|---|
| 닉네임·지역·밴드 이름/소개·지원 메시지 | **거절** (400) | 남에게 오래 보이는 곳, 고쳐서 다시 쓰면 됨 |
| 채팅 | **`***`로 가림** | 대화 흐름을 끊지 않음 |

- `시 발` 처럼 한 글자씩 띄운 것은 붙여서 검사하고, `시발점`·`시바견` 같은 정상 단어는 허용 목록으로 뺐다.
- 단어 목록은 `resources/moderation/*.txt`로 코드와 분리했다.

### 6. 차단
차단하면 ① 그 사람의 채팅이 내 화면에서 사라지고 ② 실시간으로도 안 오며 ③ 그 사람이 방장인 밴드가 탐색에 안 보인다.
같은 밴드 활동(합주·회비)은 막지 않는다 — 밴드 운영이 깨지지 않게 "보이는 것"만 줄였다.

## 테스트
- `AppReleaseApiTest`: 앱 토큰으로 세션·CSRF 없이 호출, SSE 토큰, 로그아웃 후 401, Apple 로그인(가짜 검증기),
  1회용 코드 재사용 거절, 기기 등록과 탈퇴 시 정리, CORS 허용/거절, 차단, 욕설 거절·가림, 문의 이메일
- `ProfanityFilterTest`: 띄어쓰기 우회, 허용 단어, 가리기
- iOS 빌드 자체는 macOS가 필요해 수동 워크플로로 분리했다 (Apple 계정 설정 후 실행)

## 사람이 할 일
[iOS 출시 가이드](ios-release.md) — Apple 개발자 가입, 키 등록, Firebase, 심사 제출
