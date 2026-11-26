# 밴드몽 작업 안내서 (AI 에이전트·새 개발자용)

직장인 밴드 매칭·운영 앱. Spring Boot API + React(모바일 웹) + Capacitor iOS 앱.
이 파일은 "여기서 일하는 법"만 적는다. 기능 설명은 README.md, 설계 이유는 docs/stage-0N.md.

## 끝났다고 말하기 전에

```bash
./verify.sh          # 전부. 통과해야 "완료"
./verify.sh front    # 프론트만 (몇 초)
./verify.sh rules    # 구조 규칙만 (ArchUnit·스키마·ESLint)
```
실패 메시지에는 **고치는 법**이 적혀 있다. 규칙을 끄거나 예외를 늘리기 전에 그 방법대로 고친다.
규칙 자체가 틀렸다고 판단되면 바꾸지 말고 이유와 함께 사람에게 묻는다.

## 구조

```
backend/src/main/java/com/bandmong/
  band jam gig studio dues   기능 (서비스·엔티티·컨트롤러가 기능별로 한 패키지)
  activity                   Events(일어난 일) + ActivityListener(알림·채팅 안내를 여기서만 결정)
  notification chat stream   알림 저장 / 밴드 채팅 / SSE 실시간
  moderation                 신고·이용 제한·차단
  app                        앱 전용: Bearer 토큰, Apple 로그인, 푸시(FCM)
  auth user config web       로그인 / 계정 / 보안 설정 / 공용(ApiException, ProfanityFilter)
backend/src/main/resources/db/migration   Flyway (운영 MySQL 전용)
backend/src/test/java/com/bandmong/harness 구조 규칙 테스트
frontend/src/api        서버 호출 (client.ts의 request()만 fetch를 쓴다)
frontend/src/lib        time(한국 시간) stream(SSE) platform·native(앱 전용)
frontend/ios            Capacitor iOS 프로젝트
```

## 규칙 (자동 검사됨 — 위반하면 테스트/린트가 실패)

| 규칙 | 검사 | 이유 |
|---|---|---|
| 기능 코드(band·jam·gig·studio·dues)는 알림·SSE·푸시·채팅 안내를 직접 부르지 않는다. `Events`에 record 추가 → `publishEvent` → `ActivityListener` | ArchitectureRulesTest | 알림 정책을 한 곳에서 관리 |
| 현재 시각은 주입받은 `Clock`으로만 (`Instant.now()` 금지, 엔티티는 `Instant now` 파라미터) | ArchitectureRulesTest | 테스트가 시간을 고정함 (`MutableClock`) |
| 컨트롤러는 리포지토리를 직접 쓰지 않는다 | ArchitectureRulesTest | 권한·트랜잭션은 서비스에서 |
| `web`·`stream`은 기능 패키지를 import하지 않는다 | ArchitectureRulesTest | 바닥 패키지 |
| 생성자 주입만 (`@Autowired` 필드 금지) | ArchitectureRulesTest | |
| 엔티티 필드를 추가·변경하면 **새** `V{n}__설명.sql`을 만든다 (기존 파일 수정 금지) | SchemaRulesTest + CI mysql 잡 | 운영은 `ddl-auto: validate` — 빠뜨리면 서버가 안 뜬다 |
| 테이블·컬럼 이름에 SQL 예약어 금지 (`month`, `user`, `order`, `key`, `value`…) | SchemaRulesTest | H2/MySQL 문법 오류 |
| 테스트에서 응답 본문은 UTF-8로 (`getContentAsString(UTF_8)` 또는 `ApiTestSupport.read`) | ArchitectureRulesTest | 한글 메시지가 깨짐 |
| 프론트: `fetch`는 api/client.ts만, `EventSource`는 lib/stream.tsx만, 한국 시간 계산은 lib/time.ts만, `localStorage`·`alert` 금지, Capacitor import는 lib/native·platform만 | ESLint | 웹/앱 겸용 유지 |

## 규칙 (사람 판단 — 검사 안 됨)

- 사용자에게 보이는 글(오류 메시지 포함)은 **한국어 존댓말 구어체**: "다시 로그인해 주세요", "방장만 할 수 있어요"
- 오류는 `throw new ApiException(HttpStatus.X, "메시지")` — 프론트가 메시지를 그대로 띄운다
- 사용자가 쓰는 글(닉네임·밴드 소개·지원 메시지)은 `profanity.check(...)`, 채팅은 `profanity.mask(...)`
- 동시에 같은 자원을 바꿀 수 있는 곳은 잠금 방식을 정하고 두 스레드 테스트를 쓴다 (docs/stage-05.md)
- 새 API는 웹(세션+CSRF)과 앱(Bearer) 둘 다에서 돈다. `/api/**` 아래에 두면 자동으로 둘 다 된다
- 시간: DB는 UTC, "오늘·이번 주·요일" 판단은 한국 시간(Asia/Seoul)

## 테스트 쓰는 법

- API 테스트는 `ApiTestSupport`를 상속: `loginAs("이름")`, `bandWithMember(...)`, `expect(...)`, `read(...)`
- 시간이 걸린 동작은 `MutableClock`으로 시각을 옮겨 검증
- 테스트 이름은 한국어 문장: `void 마지막_자리에_동시_수락하면_한_명만_성공()`

## 명령

| 하는 일 | 명령 |
|---|---|
| 백엔드 실행 (H2, 개발용 로그인) | `cd backend && ./gradlew bootRun` |
| 프론트 실행 | `cd frontend && npm run dev` → http://localhost:5173 |
| 테스트 하나 | `cd backend && ./gradlew test --tests '*JamApiTest'` |
| 운영과 같은 구성 | `docker compose up --build` → http://localhost:8000 |
| 마이그레이션 초안 | Actions > Schema export (엔티티 → MySQL CREATE 문) |

## 과거에 실제로 났던 실수 (그래서 위 검사가 생겼다)

- 컬럼 이름 `month` → H2 테스트 전체 실패 → `due_month`로 변경 (SchemaRulesTest 예약어 검사)
- MockMvc가 본문을 ISO-8859-1로 읽어 한글 비교 실패 (UTF-8 규칙)
- 엔티티 11곳(정산·회비 납부·지원 결정·공연 수정 등)이 `Instant.now()`를 직접 써서 시간 고정 테스트가 안 먹혔음 (Clock 규칙이 처음 돌 때 찾아냄)
- 큐시트 버전 증가를 `updatedAt` 시각 변경에 기대고 있었음 → 시계를 고정하면 동시 저장 충돌을 못 잡음. 지금은 `OPTIMISTIC_FORCE_INCREMENT`
- 한국 시간 `+ 9 * 3600_000` 계산이 화면 5곳에 복사돼 있었음 → lib/time.ts로 모음 (ESLint)

## 하지 말 것

- 이미 배포된 마이그레이션(V1, V2…) 수정
- `.env`, `*.p8`, `GoogleService-Info.plist`, 서비스 계정 JSON 커밋
- 테스트를 통과시키려고 검사·테스트 끄기 (`@Disabled`, eslint-disable, 규칙 예외 추가)
- main 브랜치에 CI 실패 상태로 두기
