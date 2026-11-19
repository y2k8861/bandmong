# 2단계 — 밴드 만들기·찾기·지원

## 만든 것

| 기능 | API | 화면 |
| --- | --- | --- |
| 밴드 만들기 | `POST /api/bands` | 밴드 만들기 |
| 밴드 찾기 (장르·빈자리 악기·지역) | `GET /api/bands?genre=&instrument=&area=` | 찾기 탭 |
| 밴드 상세 | `GET /api/bands/{id}` | 상세 |
| 내 밴드 + 지원한 밴드 | `GET /api/me/bands` | 내 밴드 탭 |
| 지원 / 지원 취소 | `POST /api/bands/{id}/applications`, `DELETE …/applications/mine` | 상세 → 지원 시트 |
| 수락 / 거절 (방장) | `POST …/applications/{appId}/accept`, `…/reject` | 상세 → 지원자 |
| 자리·모집·소개 수정 (방장) | `PATCH /api/bands/{id}` | 상세 → 자리·모집 설정 |
| 멤버 나가기 | `DELETE /api/bands/{id}/members/me` | 상세 |

### 테이블

```
bands              id, title, genre, level, area, schedule, intro, recruiting, host_id, created_at
band_seats         band_id, instrument, seat_count          ← 악기별 자리 수
band_members       id, band_id, user_id, instrument, role(HOST/MEMBER), joined_at   (band_id,user_id 유니크)
band_applications  id, band_id, user_id, instrument, message, status, created_at, decided_at
```

## 왜 이렇게 만들었나

### 1. 빈자리는 저장하지 않고 계산한다
- 저장하는 건 "악기별 자리 수"뿐이고, 빈자리 = 자리 수 − 그 악기 멤버 수.
- "빈자리 수" 칸을 따로 두면 수락·나가기·자리 수정 때마다 같이 고쳐야 하고, 하나라도 빠지면 숫자가 틀어진다.
- 방장 자리도 자리 수에 포함된다 (프로토타입과 같은 규칙). 규칙: 악기당 3자리, 전체 8자리.

### 2. 마지막 자리 동시 수락 — 밴드 행 비관적 잠금
- 문제: 베이스 1자리에 두 명이 지원했고, 방장이 휴대폰과 PC에서 거의 동시에 둘 다 수락한다.
  두 요청이 모두 "빈자리 1개"를 읽고 둘 다 멤버를 넣으면 자리보다 사람이 많아진다.
- 해결: 자리를 바꾸는 작업(지원·수락·자리 수정·나가기)은 `SELECT … FOR UPDATE`로 **밴드 행을 먼저 잠그고**,
  그다음 지원서와 멤버 수를 다시 읽어 확인한다. 두 번째 요청은 첫 번째가 끝날 때까지 기다렸다가
  "베이스 빈자리가 방금 찼어요"로 409를 받는다.
- 왜 낙관적 잠금(@Version)이 아니라 비관적 잠금인가: 충돌이 나면 결국 사용자에게 실패를 알려야 하는 건 같지만,
  같은 밴드에 몰리는 요청은 방장 한 명의 조작이라 대기 시간이 짧고, 재시도 로직 없이 결과가 명확하다.
- `LastSeatConcurrencyTest`가 두 스레드로 동시에 수락해 정확히 한 명만 들어가는지 확인한다.

### 3. 지원 중복도 같은 잠금으로 막는다
- "대기 중 지원은 밴드당 1개"는 상태(PENDING)에 걸린 조건이라 단순 유니크 키로 표현하기 어렵다.
- 지원도 밴드 행을 잠근 뒤 확인하므로, 같은 사람이 버튼을 두 번 눌러도 두 번째는 409.

### 4. 지원서는 지우지 않고 상태만 바꾼다
- PENDING → ACCEPTED / REJECTED / CANCELED, `decided_at` 기록.
- 나중에 "거절된 사람이 같은 밴드에 몇 번 지원했나" 같은 운영 판단, 신고 처리 때 근거가 된다.

### 5. 목록 조회의 N+1 막기
- 찾기 목록은 밴드 100개 × (멤버, 자리)를 보여 준다. 밴드마다 따로 조회하면 쿼리가 200개 넘게 나간다.
- 멤버는 `where band_id in (…)` 한 번으로 가져와 묶고, 자리(`band_seats`)는 `@BatchSize(100)`로 묶어 읽는다.
- 목록은 지금 최신 100개까지. 밴드가 많아지면 커서 기반 페이지로 바꾼다.

### 6. 화면에서 먼저 막고, 서버가 최종 판정
- 프론트 `seatError()`와 서버 `validateSeats()`가 같은 규칙·같은 문구를 쓴다.
- 그래도 서버가 거절하면(예: 그 사이 자리가 참) 서버 문구를 토스트로 보여 주고 상세를 다시 읽어 최신 상태로 맞춘다.

## 테스트

- 백엔드 `BandApiTest`: 만들기, 자리 규칙, 지원→수락 전체 흐름, 지원 조건(내 악기·빈자리·모집 마감),
  거절·지원 취소, 찾기 필터, 자리 줄이기 제한, 나가기, 프로필 전 차단.
- 백엔드 `LastSeatConcurrencyTest`: 마지막 자리 동시 수락 → 정확히 1명.
- 프론트: 자리 규칙·시간 표시, 상세에서 지원 보내기, 지원 불가 버튼, 방장 수락 버튼 비활성·서버 거절 문구 표시.

## 다음 단계로 넘긴 것

- 방장 넘기기, 밴드 해산 (지금은 방장이 나갈 수 없음)
- 지원 결과 알림 (4단계 알림에서)
