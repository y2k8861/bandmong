# 밴드몽 🎸

퇴근한 직장인이 회사 근처에서 같이 합주할 사람을 찾고, 합주 일정과 밴드 살림(회비·예약·공연 준비)을 한곳에서 관리하는 모바일 웹 서비스.

> HTML 한 파일로 만든 프로토타입을 **Spring Boot + React** 실제 서비스 구조로 옮기는 중입니다.
> 단계별로 무엇을 왜 그렇게 만들었는지는 [`docs/`](docs/)에 적어 둡니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 백엔드 | Java 21, Spring Boot 3.3, Spring Security(세션 + CSRF 쿠키, OAuth2 로그인), Spring Data JPA |
| DB | 로컬 H2(메모리) / 운영 MySQL |
| 프론트 | React 18, TypeScript, Vite, React Router |
| 테스트 | JUnit 5 + MockMvc, Vitest + Testing Library |
| 실시간 | SSE(Server-Sent Events) — 채팅·알림 |
| DB 마이그레이션 | Flyway (운영 MySQL은 `ddl-auto: validate`) |
| 배포 | Docker, nginx, Caddy(https), GHCR, AWS EC2 + RDS |
| CI/CD | GitHub Actions: 테스트 → MySQL 검증 → 전체 묶음 스모크 테스트 → 이미지 푸시 → (수동) EC2 배포 |

## 폴더 구조

```
bandmong/
├── backend/    Spring Boot API 서버 (포트 8080)
├── frontend/   React 앱 (개발 서버 5173, /api는 8080으로 프록시)
└── docs/       단계별 설계 기록
```

## 로컬에서 실행하기

필요한 것: JDK 21, Node 22

```bash
# 1) 백엔드 — dev 프로필(H2 메모리 DB, 개발용 로그인)으로 뜬다
cd backend
./gradlew bootRun

# 2) 프론트 — 다른 터미널에서
cd frontend
npm install
npm run dev
```

브라우저에서 http://localhost:5173 을 열고 **개발용 로그인**에 아무 이름이나 넣으면 됩니다.
같은 이름으로 다시 들어오면 같은 계정이에요.

테스트: `cd backend && ./gradlew test`, `cd frontend && npm test`

### 운영과 같은 구성으로 한 번에 띄우기 (Docker)

```bash
docker compose up --build      # MySQL + Spring + nginx → http://localhost:8000
deploy/smoke-test.sh           # 로그인·한글·실시간 연결까지 자동 확인
```

## 배포

AWS EC2 + RDS(MySQL) + Docker + GitHub Actions 구성으로 준비되어 있다. → [배포 가이드](docs/deploy.md)

- push마다 CI가 ① 백엔드·프론트 테스트 ② **실제 MySQL**에 운영 설정으로 붙여 마이그레이션 검증
  ③ docker compose로 전체를 띄워 실제 요청 확인 ④ 이미지를 GHCR에 푸시
- 배포는 Actions의 Deploy를 손으로 실행 (이미지 태그로 되돌리기 가능)

## 진행 상황

- [x] **1단계** 프로젝트 뼈대, 로그인(개발용 + 카카오·네이버·구글 준비), 프로필 설정 — [설계 기록](docs/stage-01.md)
- [x] **2단계** 밴드 만들기·찾기·지원, 방장 수락/거절, 마지막 자리 동시 수락 방지 — [설계 기록](docs/stage-02.md)
- [x] **3단계** 가능 시간 투표, 합주 확정·변경·취소, 참석 응답 — [설계 기록](docs/stage-03.md)
- [x] **4단계** 밴드 채팅, 알림, 실시간 연결(SSE) — [설계 기록](docs/stage-04.md)
- [x] **5단계** 합주실 예약·1인 정산·월 회비, 공연 큐시트·체크리스트 — [설계 기록](docs/stage-05.md)
- [x] **6단계** 신고·관리자 처리·이용 제한, 방장 넘기기·탈퇴, 배포 준비(Flyway·Docker·CI/CD·AWS 가이드) — [설계 기록](docs/stage-06.md)
