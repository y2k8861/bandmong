# iOS 앱 출시 가이드 (App Store)

밴드몽 iOS 앱은 **Capacitor**로 지금의 React 화면을 그대로 감싼 앱입니다.
맥이 없어도 GitHub Actions의 macOS 머신에서 빌드·서명·업로드까지 합니다.
코드 쪽 준비는 끝나 있고, 아래는 **사람이 직접 해야 하는 일**입니다.

> 순서 요약: ① Apple 개발자 가입 → ② 키·설정 만들어 GitHub에 넣기 → ③ 서버 환경변수 → ④ Actions 실행(TestFlight) → ⑤ 심사 제출

---

## 0. 코드에 이미 들어간 것 (심사 요건 대응)

| 애플 요건 | 밴드몽에서 |
|---|---|
| 4.8 소셜 로그인이 있으면 Apple 로그인도 | 앱 로그인 화면 맨 위 "Apple로 계속하기" |
| 5.1.1(v) 앱 안에서 계정 삭제 | 내 정보 > 회원 탈퇴 (모든 기기 토큰·푸시 토큰 즉시 삭제) |
| 1.2 사용자 콘텐츠(채팅 등) | 욕설 자동 가림·거절, 메시지/사용자 신고, 사용자 차단, 앱 안 문의 이메일, 약관에 무관용 원칙 |
| 4.2 단순 웹 포장 금지 | 푸시 알림, Apple 로그인, 앱 전용 로그인 흐름 등 네이티브 기능 |
| 개인정보 처리방침 URL | `https://<도메인>/privacy.html` (이용약관 `/terms.html`) — **초안이니 검토 필요** |

구조 메모: 앱은 `capacitor://localhost`에서 돌아가므로 서버와 출처가 달라 쿠키 세션을 쓸 수 없습니다.
그래서 앱은 `Authorization: Bearer` 앱 토큰(서버에는 SHA-256 해시만 저장)을 쓰고, 웹은 기존 세션+CSRF를 그대로 씁니다.

---

## 1. Apple 개발자 프로그램 가입 (연 $99, 129,000원 안팎)

1. https://developer.apple.com/programs/ 에서 개인으로 가입 (승인 1~2일)
2. **Certificates, Identifiers & Profiles > Identifiers**에서 App ID 만들기
   - Bundle ID: `com.bandmong.app` (다른 값을 쓰려면 `capacitor.config.ts`, Xcode 프로젝트, 서버 `APPLE_BUNDLE_ID`를 함께 바꿔야 함)
   - Capabilities: **Sign In with Apple**, **Push Notifications** 체크
3. **Keys**에서 APNs 키 만들기 (Apple Push Notifications service 체크) → `.p8` 다운로드, Key ID 메모
4. **App Store Connect > 앱 > +** 로 새 앱 등록 (이름 "밴드몽", 기본 언어 한국어, 위 Bundle ID 선택)

## 2. GitHub에 넣을 값

### App Store Connect API 키
App Store Connect > 사용자 및 액세스 > 통합 > App Store Connect API > 키 생성
- 역할: **관리자(Admin)** — Xcode가 배포 인증서를 자동으로 만들려면 필요
- `.p8` 파일은 한 번만 내려받을 수 있으니 잘 보관

### Firebase (푸시 알림)
1. https://console.firebase.google.com 에서 프로젝트 만들기
2. iOS 앱 추가 (Bundle ID `com.bandmong.app`) → `GoogleService-Info.plist` 다운로드
3. 프로젝트 설정 > 클라우드 메시징 > Apple 앱 구성 > **1단계에서 만든 APNs 키(.p8) 업로드**
4. 프로젝트 설정 > 서비스 계정 > **새 비공개 키 생성** → 서버용 JSON (3단계에서 사용)

### 저장소에 등록 (Settings > Secrets and variables > Actions)

| 종류 | 이름 | 값 |
|---|---|---|
| Secret | `APPLE_TEAM_ID` | developer.apple.com > Membership의 Team ID (10자리) |
| Secret | `ASC_KEY_ID` | API 키의 Key ID |
| Secret | `ASC_ISSUER_ID` | API 키 화면 위쪽의 Issuer ID |
| Secret | `ASC_KEY_P8` | `.p8` 파일 내용 전체 (BEGIN/END 줄 포함) |
| Secret | `GOOGLE_SERVICE_INFO_PLIST` | `base64 -i GoogleService-Info.plist` 결과 |
| Variable | `API_BASE` | 서버 주소, 예: `https://bandmong.example.com` (끝에 / 없이) |

## 3. 서버 환경변수 (`deploy/.env`)

```
APPLE_BUNDLE_ID=com.bandmong.app
SUPPORT_EMAIL=본인이 받을 이메일          # 앱 로그인·내 정보 화면에 표시
FCM_CREDENTIALS=/run/secrets/fcm.json     # 2단계 서비스 계정 JSON 경로 (비우면 푸시 끔)
```
- `FCM_CREDENTIALS` 파일은 백엔드 컨테이너에 볼륨으로 넣어 주세요.
- 소셜 로그인(카카오·네이버·구글) 개발자 콘솔의 Redirect URI는 웹과 같아서 바꿀 것이 없습니다.
  앱에서는 `?app=1`로 시작해 로그인이 끝나면 `bandmong://auth?code=…`로 돌아옵니다.

## 4. TestFlight 빌드

GitHub > Actions > **iOS TestFlight** > Run workflow (버전 입력, 예 `1.0.0`)
- 약 15~25분 뒤 App Store Connect > TestFlight에 빌드가 보입니다.
- 빌드 번호는 실행 번호로 자동 증가합니다.
- 본인 아이폰에 TestFlight 앱을 깔고 내부 테스터로 자신을 추가하면 바로 설치됩니다.

확인할 것: Apple 로그인, 카카오 로그인 후 앱으로 돌아오는지, 알림 허용 후 다른 계정으로 채팅을 보내 푸시가 오는지, 회원 탈퇴.

## 5. 심사 제출

App Store Connect에서 채울 것:
- **스크린샷**: 6.9인치(1320×2868) 최소 3장. 시뮬레이터가 없으면 `docs/images`처럼 Playwright로 390×844 화면을 3배로 찍어 크기를 맞춰도 됩니다.
- **개인정보 처리방침 URL**: `https://<도메인>/privacy.html`
- **앱 개인정보(영양 라벨)**: 수집함 — 연락처 정보(이메일), 사용자 콘텐츠(메시지·기타), 식별자(사용자 ID, 기기 ID=푸시 토큰). 모두 "앱 기능", 추적 안 함.
- **연령 등급**: 사용자 간 무제한 채팅이 있으므로 해당 항목 "예" (보통 12+ 또는 17+)
- **심사 메모(App Review Information)** 예시:
  > Apple로 계속하기로 로그인할 수 있습니다. 로그인 후 '탐색'에서 '심사용 데모 밴드'에 가입 신청하면 바로 승인되어 합주 일정·채팅·회비 기능을 볼 수 있습니다.
  > 채팅에서 상대 메시지를 누르면 신고/차단, 내 정보에서 차단 목록·문의·회원 탈퇴가 있습니다.
  - 제출 전에 운영 서버에 데모 밴드를 하나 만들어 두고, 신청을 바로 받아 줄 수 있게 해 두세요.
- **수출 규정**: Info.plist에 `ITSAppUsesNonExemptEncryption=false`가 들어 있어 따로 묻지 않습니다 (HTTPS만 사용).

운영 약속: 약관에 "신고는 24시간 안에 확인"이라고 적었습니다. 관리자 계정으로 `/admin`(신고 처리)을 주기적으로 확인해 주세요.

## 자주 막히는 곳

| 증상 | 원인 / 해결 |
|---|---|
| Actions "설정 확인"에서 실패 | 오류 메시지에 빠진 이름이 나옵니다. 2단계 표 확인 |
| `No profiles for 'com.bandmong.app'` | App ID 미생성, 또는 API 키 역할이 Admin이 아님 |
| Apple 로그인 후 "다시 로그인해 주세요" | 서버 `APPLE_BUNDLE_ID`가 앱 Bundle ID와 다름 |
| 푸시가 안 옴 | Firebase에 APNs 키 업로드 누락, 또는 서버 `FCM_CREDENTIALS` 비어 있음 (백엔드 시작 로그에 "FCM 설정이 없어 푸시 알림을 보내지 않습니다") |
| 카카오 로그인 후 앱으로 안 돌아옴 | Info.plist URL scheme `bandmong` 확인, 서버 `APP_SCHEME` 값 확인 |
| 앱에서 API가 CORS 오류 | 서버 `bandmong.app.cors-origins`에 `capacitor://localhost` 포함 확인 |
