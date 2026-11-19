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
| CI | GitHub Actions (push마다 백엔드·프론트 테스트와 빌드) |

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

## 진행 상황

- [x] **1단계** 프로젝트 뼈대, 로그인(개발용 + 카카오·네이버·구글 준비), 프로필 설정 — [설계 기록](docs/stage-01.md)
- [x] **2단계** 밴드 만들기·찾기·지원, 방장 수락/거절, 마지막 자리 동시 수락 방지 — [설계 기록](docs/stage-02.md)
- [x] **3단계** 가능 시간 투표, 합주 확정·변경·취소, 참석 응답 — [설계 기록](docs/stage-03.md)
- [ ] 4단계 밴드 채팅·알림
- [ ] 5단계 합주실 예약·회비 정산, 공연 준비
- [ ] 6단계 신고·운영, 배포
